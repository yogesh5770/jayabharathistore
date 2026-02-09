# 📞 Twilio Voice Configuration (TwiML)

Since you want the **App -> Dialer -> Virtual Number** flow, you must configure Twilio to handle the incoming call.

### 1. Go to TwiML Bins
1.  Open [Twilio Console](https://console.twilio.com/).
2.  Search for **"TwiML Bins"** in the top search bar.
3.  Click **"Create New TwiML Bin"**.

### 2. Paste This Code
Give it a name like "Jayabharathi Call Flow" and paste this exactly:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say>Welcome to Jayabharathi Store. Connecting your call.</Say>
    <Dial>+917010196231</Dial>
</Response>
```

4.  Click **Create** / **Save**.

### 3. Link it to Your Number
1.  Go to **Phone Numbers > Manage > Active Numbers**.
2.  Click on your number: `+1 251 766 5251`.
3.  Scroll down to **"Voice & Fax"**.
4.  Under **"A Call Comes In"**:
    *   Select **"TwiML Bin"**.
    *   Select **"Jayabharathi Call Flow"** (the one you just made).
5.  Click **Save**.

### ✅ Done!
Now, when you click "Call" in the app:
1.  The Dialer opens with `+1 251 766 5251`.
2.  You press Call.
3.  You hear: *"Welcome to Jayabharathi Store..."*
4.  It connects you to the Delivery Boy (`7010196231`).

---

## ⚠️ CRITICAL: Trial Limitations (Read This)

Since you are using a **Free Trial Account**, there are two big rules:

### 1. Delivery Boy MUST be Verified
*   The number you put inside `<Dial>...</Dial>` (which is `7010196231`) **MUST** be verified in your Twilio Console.
*   If you change the delivery boy to a different friend, you **MUST verify their number first**, or the call will fail.

### 2. Users calling "International" (+1 US Number)
*   Your Virtual Number is from the **USA** (`+1...`).
*   **For You/Professor:** It is fine for testing.
*   **For Real Users:** Calling a US number from India costs money (ISD rates).
*   **Fix for Project:** Just explain, *"In a real production app, we would buy a +91 Indian number, but for this demo, we are using a US Trial number."*

