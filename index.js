export default {
  async fetch(request, env) {
    // 1. Handle CORS Preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type, Authorization",
        },
      });
    }

    if (request.method !== "POST") {
      return new Response("Method Not Allowed. Use POST.", {
        status: 405,
        headers: { "Access-Control-Allow-Origin": "*" }
      });
    }

    try {
      // 2. Validate Request Content
      const contentType = request.headers.get("content-type") || "";
      if (!contentType.includes("application/json")) {
        return new Response(JSON.stringify({ success: false, error: "Content-Type must be application/json" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      let body;
      try {
        body = await request.json();
      } catch (e) {
        return new Response(JSON.stringify({ success: false, error: "Invalid JSON body provided" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      const { user_phone, delivery_boy_phone, caller_role } = body;

      if (!user_phone || !delivery_boy_phone) {
        return new Response(JSON.stringify({ success: false, error: "Missing phone numbers" }), {
          status: 400,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" }
        });
      }

      // 3. Twilio Configuration Check
      const accountSid = env.TWILIO_ACCOUNT_SID;
      const authToken = env.TWILIO_AUTH_TOKEN;
      const twilioNumber = env.TWILIO_PHONE_NUMBER;

      if (!accountSid || !authToken || !twilioNumber) {
        return new Response(JSON.stringify({ success: false, error: "Worker Configuration Error: Missing Credentials" }), {
          status: 500,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      const endpoint = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Calls.json`;

      // 4. Logic for Dynamic Greetings and Call Order
      // CRITICAL: The person specified in caller_role initiated the call
      // So THEY should receive the call first, hear the greeting, then get connected
      let initiatorPhone = user_phone;  // Person who clicked "Call" button
      let recipientPhone = delivery_boy_phone;  // Person to connect TO
      let greeting = "Welcome to Jayabharathi Store. Please wait while we connect you to your delivery partner.";

      if (caller_role === "delivery") {
        initiatorPhone = delivery_boy_phone;  // Delivery partner initiated
        recipientPhone = user_phone;  // Connect to customer
        greeting = "Please wait while I'm connecting you to the customer.";
      }

      // TwiML: The initiator hears the greeting, then we dial the recipient
      const twiml = `<Response>
                       <Say voice="woman" language="en-IN">${greeting}</Say>
                       <Dial callerId="${twilioNumber}" timeout="30">${recipientPhone}</Dial>
                     </Response>`;

      const formData = new URLSearchParams();
      formData.append("To", initiatorPhone);  // Call the person who clicked the button FIRST
      formData.append("From", twilioNumber);
      formData.append("Twiml", twiml);

      // 5. Call Twilio
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": "Basic " + btoa(`${accountSid}:${authToken}`),
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        return new Response(JSON.stringify({
          success: false,
          error: data.message || "Twilio API Error"
        }), {
          status: response.status,
          headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
        });
      }

      return new Response(JSON.stringify({
        success: true,
        sid: data.sid,
        message: "Call initiated successfully"
      }), {
        status: 200,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });

    } catch (err) {
      return new Response(JSON.stringify({ success: false, error: "Worker Error: " + err.message }), {
        status: 500,
        headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
      });
    }
  },
};
