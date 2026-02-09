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

      // ---------------------------------------------------------
      // 🔑 HARDCODED CREDENTIALS (Easier for you to Copy-Paste)
      // ---------------------------------------------------------
      const apiKey = "9f045f5d164d14c6a418c511b8191f0a23fdd293c9f95688";
      const apiToken = "93c22f760c76ca754d40acd383e7b0dc10fe892a8e9c8e53";
      const accountSid = "jayabharathistore1";
      const exophone = "04446312684"; 
      // ---------------------------------------------------------

      // Exotel API Endpoint (Direct Connect)
      const endpoint = `https://api.exotel.com/v1/Accounts/${accountSid}/Calls/connect.json`;

      // Form Data for Exotel API (Direct Connect)
      const formData = new URLSearchParams();
      formData.append("From", user_phone); // Call user first
      formData.append("To", delivery_boy_phone); // Then connect to delivery boy
      formData.append("CallerId", exophone); // The Virtual Number
      formData.append("CallType", "trans"); // Transactional

      // Call Exotel API
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": "Basic " + btoa(`${apiKey}:${apiToken}`),
          "Content-Type": "application/x-www-form-urlencoded",
        },
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        return new Response(JSON.stringify({ success: false, error: JSON.stringify(data) }), {
          status: 500,
          headers: { "Content-Type": "application/json" },
        });
      }

      return new Response(JSON.stringify({ success: true, sid: data.Call?.Sid }), {
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
