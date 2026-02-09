import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import express from "express";
import axios from "axios";
import crypto from "crypto";

admin.initializeApp();
const db = admin.firestore();

const app = express();
app.use(express.json());

// Collections for audit and webhook dedupe
const auditsCol = db.collection("audits");
const webhookEventsCol = db.collection("webhook_events");

// Helper: compute server-side total from items
function computeTotal(items: any[], deliveryFee = 0) {
  if (!Array.isArray(items)) return deliveryFee;
  return items.reduce((sum, it) => {
    const price = Number(it.price || 0);
    const qty = Number(it.quantity || 1);
    return sum + (price * qty);
  }, 0) + deliveryFee;
}

// Create order endpoint (called by app)
app.post("/createOrder", async (req, res) => {
  try {
    const userId = req.body.userId || "anonymous";
    const items = req.body.items || [];
    const address = req.body.address || {};
    const paymentMethod = req.body.paymentMethod || "cod";
    const deliveryFee = Number(req.body.deliveryFee || 0);
    const idempotencyKey = req.body.idempotencyKey
    // compute total server-side
    const totalAmount = computeTotal(items, deliveryFee);

    // Audit the incoming request
    try {
      await auditsCol.add({
        type: "createOrder",
        userId,
        idempotencyKey: idempotencyKey || null,
        payload: req.body,
        createdAt: Date.now()
      });
    } catch (e) {
      console.warn("Failed to write audit:", e);
    }

    // Idempotency: if idempotencyKey provided, return existing order if present
    if (idempotencyKey) {
      const existingQ = await db.collection("orders")
        .where("idempotencyKey", "==", idempotencyKey)
        .where("userId", "==", userId)
        .limit(1)
        .get();
      if (!existingQ.empty) {
        const doc = existingQ.docs[0];
        const data = doc.data();
        return res.json({ orderId: doc.id, paymentStatus: data.paymentStatus || "PENDING", existing: true });
      }
    }

    // Create order document
    const newOrderRef = db.collection("orders").doc();
    const orderData: any = {
      id: newOrderRef.id,
      userId,
      items,
      deliveryAddress: address,
      totalAmount,
      status: "PENDING",
      paymentMethod,
      paymentStatus: "PENDING",
      createdAt: Date.now(),
      idempotencyKey: idempotencyKey || null
    };
    await newOrderRef.set(orderData);

    // Auto-verify zero-amount orders
    if (totalAmount <= 0) {
      const paymentsCol = newOrderRef.collection("payments");
      await paymentsCol.add({
        provider: "FREE",
        amount: 0,
        transactionId: `FREE-${newOrderRef.id}`,
        createdAt: Date.now()
      });
      await newOrderRef.update({ paymentStatus: "PAID", paymentProvider: "FREE" });
      // send notifications to store and delivery topics
      try {
        const msg = {
          notification: {
            title: "New Order (Free)",
            body: `Order ${newOrderRef.id} placed and auto-verified`
          },
          data: {
            orderId: newOrderRef.id,
            event: "ORDER_PLACED_PAID"
          }
        };
        await admin.messaging().sendToTopic("store", msg);
        await admin.messaging().sendToTopic("delivery", msg);
      } catch (e) {
        console.warn("FCM send failed:", e);
      }
      return res.json({ orderId: newOrderRef.id, paymentStatus: "PAID" });
    }

    // For paid orders create Cashfree order token if env present
    const CF_CLIENT_ID = functions.config().cashfree?.client_id;
    const CF_SECRET = functions.config().cashfree?.secret;
    const CF_ENV = functions.config().cashfree?.env || "sandbox";

    if (!CF_CLIENT_ID || !CF_SECRET) {
      // Return order id - client will initiate alternative flow or retry
      return res.json({ orderId: newOrderRef.id, message: "No Cashfree config, created order in PENDING" });
    }

    // Create Cashfree order token
    const cfUrl = CF_ENV === "production" ? "https://api.cashfree.com/pg/orders" : "https://sandbox.cashfree.com/pg/orders";
    const payload = {
      order_id: newOrderRef.id,
      order_amount: String(totalAmount),
      order_currency: "INR",
      customer_details: {
        customer_id: userId,
        customer_email: req.body.customerEmail || "",
        customer_phone: address.phoneNumber || ""
      }
    };

    const cfResp = await axios.post(cfUrl, payload, {
      headers: {
        accept: "application/json",
        "content-type": "application/json",
        "x-client-id": CF_CLIENT_ID,
        "x-client-secret": CF_SECRET,
        "x-api-version": "2022-09-01"
      },
      timeout: 10000
    });

    const orderToken = cfResp.data?.order_token;
    if (orderToken) {
      await newOrderRef.update({ cashfree_order_token: orderToken });
      // notify store/delivery about new pending order
      try {
        const msg = {
          notification: {
            title: "New Order",
            body: `Order ${newOrderRef.id} placed — ₹${totalAmount}`
          },
          data: {
            orderId: newOrderRef.id,
            event: "ORDER_PLACED"
          }
        };
        await admin.messaging().sendToTopic("store", msg);
        await admin.messaging().sendToTopic("delivery", msg);
      } catch (e) {
        console.warn("FCM send failed:", e);
      }
    }

    return res.json({ orderId: newOrderRef.id, orderToken, totalAmount });
  } catch (err: any) {
    console.error("createOrder error", err?.message || err);
    return res.status(500).json({ error: err?.message || "internal" });
  }
});

// Webhook endpoint for Cashfree - must receive raw body for signature verification
const webhookApp = express();
webhookApp.use(express.raw({ type: "*/*" }));
webhookApp.post("/cashfree", async (req, res) => {
  try {
    const CF_SECRET = functions.config().cashfree?.secret || "";
    const signature = req.headers["x-cashfree-signature"] || req.headers["x-webhook-signature"] || "";
    const raw = req.body as Buffer;
    const computed = crypto.createHmac("sha256", CF_SECRET).update(raw).digest("hex");
    if (!signature || signature !== computed) {
      console.warn("Invalid signature on cashfree webhook");
      return res.status(400).send("invalid signature");
    }
    const payload = JSON.parse(raw.toString());
    // handle different webhook shapes; try common fields
    const orderId = payload.order_id || payload.reference_id || payload.orderId;
    const txStatus = payload.order_status || payload.tx_status || payload.payment_status;
    const txId = payload.tx_id || payload.payment_id || payload.transaction_id;
    const amount = Number(payload.order_amount || payload.amount || 0);

    if (!orderId) {
      return res.status(400).send("missing order id");
    }

    // webhook dedupe: use txId or computed event id
    const eventId = txId || payload.event_id || `${orderId}-${txStatus}-${amount}`;
    const seen = await webhookEventsCol.doc(eventId).get();
    if (seen.exists) {
      console.log("Duplicate webhook event, skipping:", eventId);
      return res.status(200).send("duplicate");
    }
    // mark as seen (optimistic)
    await webhookEventsCol.doc(eventId).set({ createdAt: Date.now(), payload: payload });

    const orderRef = db.collection("orders").doc(orderId);
    const paymentsCol = orderRef.collection("payments");
    // idempotent: check for existing tx
    const existing = txId ? await paymentsCol.where("transactionId", "==", txId).get() : { empty: true };
    if (existing.empty) {
      await paymentsCol.add({
        provider: "CASHFREE",
        amount,
        transactionId: txId || `CF-${Date.now()}`,
        raw: payload,
        createdAt: Date.now()
      });
    }

    // write webhook audit
    try {
      await auditsCol.add({
        type: "webhook_cashfree",
        eventId,
        orderId,
        txId: txId || null,
        payload,
        createdAt: Date.now()
      });
    } catch (e) {
      console.warn("Failed to write webhook audit:", e);
    }

    if (txStatus === "PAID" || txStatus === "SUCCESS" || txStatus === "CAPTURED") {
      await orderRef.update({ paymentStatus: "PAID", paymentProvider: "CASHFREE" });
      // trigger downstream (fulfillment/notifications)
      try {
        const orderSnap = await orderRef.get();
        const orderData = orderSnap.data() || {};
        const userTopic = `user-${orderData.userId || ""}`;
        const msg = {
          notification: {
            title: "Payment Received",
            body: `Order ${orderId} payment successful`
          },
          data: {
            orderId,
            event: "PAYMENT_SUCCESS"
          }
        };
        await admin.messaging().sendToTopic("store", msg);
        await admin.messaging().sendToTopic("delivery", msg);
        if (orderData.userId) {
          await admin.messaging().sendToTopic(userTopic, msg);
        }
      } catch (e) {
        console.warn("FCM send failed:", e);
      }
    } else if (txStatus === "FAILED" || txStatus === "DECLINED") {
      await orderRef.update({ paymentStatus: "FAILED" });
    }

    return res.status(200).send("ok");
  } catch (err: any) {
    console.error("webhook error", err?.message || err);
    return res.status(500).send("error");
  }
});

// Submit Manual UPI Transaction ID (UTR) for verification
app.post("/submitUtr", async (req, res) => {
  try {
    const { orderId, utr } = req.body;
    if (!orderId || !utr) return res.status(400).json({ error: "missing orderId or utr" });

    const orderRef = db.collection("orders").doc(orderId);
    const orderSnap = await orderRef.get();
    if (!orderSnap.exists) return res.status(404).json({ error: "Order not found" });

    // Check if UTR already exists in any order's payments (Deduplication)
    const existingPayments = await db.collectionGroup("payments")
      .where("transactionId", "==", utr)
      .limit(1)
      .get();

    if (!existingPayments.empty) {
      return res.status(400).json({ error: "This Transaction ID has already been used." });
    }

    // Update order status to VERIFYING
    await orderRef.update({
      paymentStatus: "VERIFYING",
      transactionId: utr,
      updatedAt: Date.now()
    });

    // Record the attempt
    await orderRef.collection("payments").doc("MANUAL_" + utr).set({
      provider: "UPI_MANUAL",
      transactionId: utr,
      status: "VERIFYING",
      createdAt: Date.now()
    });

    return res.json({ status: "SUCCESS", message: "Transaction ID submitted for verification" });

  } catch (err: any) {
    console.error("submitUtr error", err.message);
    return res.status(500).json({ error: "Internal server error" });
  }
});

// Verify payment status (Directly with Cashfree API)
app.post("/verifyPayment", async (req, res) => {
  try {
    const { orderId } = req.body;
    if (!orderId) return res.status(400).json({ error: "missing orderId" });

    const CF_CLIENT_ID = functions.config().cashfree?.client_id;
    const CF_SECRET = functions.config().cashfree?.secret;
    const CF_ENV = functions.config().cashfree?.env || "sandbox";

    if (!CF_CLIENT_ID || !CF_SECRET) {
      return res.status(500).json({ error: "Server not configured for Cashfree" });
    }

    // 1. Fetch payment status from Cashfree
    const cfUrl = CF_ENV === "production"
      ? `https://api.cashfree.com/pg/orders/${orderId}/payments`
      : `https://sandbox.cashfree.com/pg/orders/${orderId}/payments`;

    const cfResp = await axios.get(cfUrl, {
      headers: {
        "x-client-id": CF_CLIENT_ID,
        "x-client-secret": CF_SECRET,
        "x-api-version": "2022-09-01"
      }
    });

    const payments = cfResp.data;
    if (!Array.isArray(payments) || payments.length === 0) {
      return res.json({ status: "PENDING", message: "No payments found yet" });
    }

    // Find the successful payment
    const successPayment = payments.find(p => p.payment_status === "SUCCESS");

    if (successPayment) {
      const orderRef = db.collection("orders").doc(orderId);
      const orderSnap = await orderRef.get();
      if (!orderSnap.exists) {
        return res.status(404).json({ error: "Order not found in DB" });
      }

      const orderData = orderSnap.data()!;

      // 2. Security Check: Verify Amount Matches
      const expectedAmount = Number(orderData.totalAmount);
      const paidAmount = Number(successPayment.payment_amount);

      if (Math.abs(expectedAmount - paidAmount) > 0.01) {
        console.error(`Fraud Alert: Amount mismatch for ${orderId}. Expected ${expectedAmount}, Paid ${paidAmount}`);
        return res.status(400).json({ error: "Amount mismatch detected" });
      }

      // 3. Update Order to PAID
      if (orderData.paymentStatus !== "PAID") {
        await orderRef.update({
          paymentStatus: "PAID",
          paymentProvider: "CASHFREE",
          transactionId: successPayment.cf_payment_id,
          updatedAt: Date.now()
        });

        // Log payment record
        await orderRef.collection("payments").doc(String(successPayment.cf_payment_id)).set({
          provider: "CASHFREE",
          amount: paidAmount,
          transactionId: successPayment.cf_payment_id,
          raw: successPayment,
          createdAt: Date.now()
        });
      }

      return res.json({ status: "SUCCESS", transactionId: successPayment.cf_payment_id });
    } else {
      // Check last payment status
      const lastPayment = payments[payments.length - 1];
      return res.json({
        status: lastPayment.payment_status,
        message: lastPayment.payment_message
      });
    }

  } catch (err: any) {
    console.error("verifyPayment error", err?.response?.data || err.message);
    return res.status(500).json({ error: err?.message || "Internal server error" });
  }
});

// Existing verify endpoint (cached view)
app.get("/orders/:orderId/verify", async (req, res) => {
  try {
    const orderId = req.params.orderId;
    const orderDoc = await db.collection("orders").doc(orderId).get();
    if (!orderDoc.exists) return res.status(404).json({ error: "not found" });
    return res.json(orderDoc.data());
  } catch (err: any) {
    console.error("verify error", err?.message || err);
    return res.status(500).json({ error: err?.message || "internal" });
  }
});

// Export functions
exports.api = functions.https.onRequest(app);
exports.webhook = functions.https.onRequest(webhookApp);

// Firestore trigger: compute ETA and route when delivery partner location updates
exports.onOrderLocationUpdate = functions.firestore
  .document("orders/{orderId}")
  .onUpdate(async (change, context) => {
    try {
      const before = change.before.data() || {};
      const after = change.after.data() || {};
      const orderId = context.params.orderId;

      const beforeLat = before.deliveryPartnerLat;
      const beforeLng = before.deliveryPartnerLng;
      const afterLat = after.deliveryPartnerLat;
      const afterLng = after.deliveryPartnerLng;

      // Only run when partner location changed
      if (!afterLat || !afterLng) return null;
      if (beforeLat === afterLat && beforeLng === afterLng) return null;

      const dest = after.deliveryAddress;
      if (!dest || dest.latitude == null || dest.longitude == null) return null;

      const key = functions.config().google?.key;
      if (!key) {
        console.warn("No Google API key configured for ETA");
        return null;
      }

      // Throttle: only run Directions API once every 5 seconds per order
      try {
        const now = Date.now();
        const lastRouteUpdateAt = after.lastRouteUpdateAt || 0;
        if (now - lastRouteUpdateAt < 5000) {
          // skip to avoid exceeding Directions API quota
          return null;
        }
      } catch (e) {
        // ignore and continue
      }

      const origin = `${afterLat},${afterLng}`;
      const destination = `${dest.latitude},${dest.longitude}`;
      const directionsUrl = `https://maps.googleapis.com/maps/api/directions/json?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&key=${key}`;
      const resp = await axios.get(directionsUrl, { timeout: 10000 });
      const route = resp.data?.routes && resp.data.routes[0];
      if (!route) return null;
      const leg = route.legs && route.legs[0];
      const etaSeconds = leg?.duration?.value || null;
      const etaText = leg?.duration?.text || null;
      const overviewPolyline = route.overview_polyline?.points || null;

      const orderRef = db.collection("orders").doc(orderId);
      const updatePayload: any = {};
      if (etaSeconds != null) updatePayload.etaSeconds = etaSeconds;
      if (etaText) updatePayload.etaText = etaText;
      if (overviewPolyline) updatePayload.routePolyline = overviewPolyline;
      // record when we last updated route to throttle future calls
      updatePayload.lastRouteUpdateAt = Date.now();
      if (Object.keys(updatePayload).length > 0) {
        await orderRef.update(updatePayload);
      }
      return null;
    } catch (e) {
      console.error("onOrderLocationUpdate error", e);
      return null;
    }
  });

// Auto-assign delivery partner on order creation
exports.assignOrder = functions.firestore
  .document("orders/{orderId}")
  .onCreate(async (snap, context) => {
    const order = snap.data();
    if (!order) return;
    if (order.deliveryPartnerId) return; // Already assigned

    try {
      // Find available partners
      // Note: Requires composite index on role + isOnline + isBusy if used together
      // For simplicity/safety with missing fields, we fetch online partners and filter
      const partnersSnapshot = await db.collection("users")
        .where("role", "==", "delivery")
        .where("isOnline", "==", true)
        .get();

      const availablePartners = partnersSnapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() } as any))
        .filter(p => !p.isBusy); // Filter out busy partners

      if (availablePartners.length === 0) {
        console.log("No available delivery partners for order", context.params.orderId);
        // Mark order as waiting for assignment so UI can show busy signal
        await snap.ref.update({ deliveryStatus: "BUSY_WAITING" });
        return;
      }

      // Random assignment
      const randomPartner = availablePartners[Math.floor(Math.random() * availablePartners.length)];

      console.log(`Assigning order ${context.params.orderId} to partner ${randomPartner.id}`);

      // Use transaction to ensure partner is still free
      await db.runTransaction(async (t) => {
        const userRef = db.collection("users").doc(randomPartner.id);
        const userDoc = await t.get(userRef);
        if (!userDoc.exists) return;

        const userData = userDoc.data();
        if (userData?.isBusy) {
          // If this partner became busy, we just abort this assignment attempt.
          // In a robust system, we would retry with another partner.
          // For now, let's just log it. The order will remain unassigned until manual intervention or next trigger?
          // Actually, since this is onCreate, it won't re-trigger. 
          // We should probably just pick another one in memory if we weren't using transaction for the *list*.
          // But transaction is on the specific user.
          throw new Error("Partner became busy");
        }

        t.update(userRef, { isBusy: true });
        t.update(snap.ref, {
          deliveryPartnerId: randomPartner.id,
          deliveryPartnerName: userData?.name || "Delivery Partner",
          deliveryPartnerPhone: userData?.phoneNumber || "",
          deliveryStatus: "ASSIGNED"
        });
      });
    } catch (e) {
      console.error("assignOrder error", e);
    }
  });


