export default {
  async fetch(request, env) {
    if (request.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405 });
    }

    try {
      const { user_phone, delivery_boy_phone } = await request.json();

      if (!user_phone || !delivery_boy_phone) {
        return new Response(JSON.stringify({ error: "Missing phone numbers" }), { status: 400 });
      }

      // Your Twilio Credentials from Worker Environment Variables
      const accountSid = env.TWILIO_ACCOUNT_SID;
      const authToken = env.TWILIO_AUTH_TOKEN;
      const twilioNumber = env.TWILIO_PHONE_NUMBER;

      const endpoint = `https://api.twilio.com/2010-04-01/Accounts/${accountSid}/Calls.json`;

      // TwiML to connect to Delivery Boy
      const twiml = `<Response>
                       <Say>Welcome to Jayabharathi Store. Connecting your call.</Say>
                       <Dial>${delivery_boy_phone}</Dial>
                     </Response>`;

      // Form Data for Twilio API
      const formData = new URLSearchParams();
      formData.append("To", user_phone);
      formData.append("From", twilioNumber);
      formData.append("Twiml", twiml);

      // Call Twilio API directly (No 'twilio' npm package needed in Workers!)
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
        return new Response(JSON.stringify({ success: false, error: data.message || "Twilio Error" }), {
          status: 500,
          headers: { "Content-Type": "application/json" },
        });
      }

      return new Response(JSON.stringify({ success: true, sid: data.sid }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });

    } catch (err) {
      return new Response(JSON.stringify({ success: false, error: err.message }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }
  },
};
