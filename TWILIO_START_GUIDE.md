# 🚀 How to Start with Twilio (Step-by-Step)

If you have decided to go with Twilio (even the Free Trial), here is exactly how to start.

## Phase 1: Get Your Credentials (5 Minutes)

1.  **Sign Up**: Go to [twilio.com/try-twilio](https://www.twilio.com/try-twilio) and create a free account.
2.  **Verify Phone**: Enter your own mobile number to verify the account.
3.  **Get a Number**:
    *   On the Console Dashboard, click **"Get a trial phone number"**.
    *   **Does this cost money?** NO. It uses your **$15 Free Trial Credit**.
    *   You do **NOT** need to pay real money to "buy" this first number.
    *   Twilio will deduct ~$1 from your *Free Credit Balance*.
    *   Twilio will give you a US number (e.g., `+1 501-234-5678`).
    *   **Save this number**. This is your "Virtual Number".
4.  **Get Keys**:
    *   Find your **Account SID** (starts with `AC...`).
    *   Find your **Auth Token**.
    *   **Save these**. You need them for the backend.

## Phase 2: Verify Numbers (CRITICAL for Trial)

In Trial Mode, you can **only** call numbers you have verified.
1.  Go to **Phone Numbers > Manage > Verified Caller IDs**.
2.  Add your Delivery Boy's phone number (`7010196231`).
3.  Add your User's phone number (the one you are testing with).
4.  Verify them via OTP.

## Phase 3: The Backend Server

You need a small server to talk to Twilio. I have created a file called `twilio-server.js` in your project folder.

1.  **Install Node.js** on your computer if not installed.
2.  Open a terminal in the project folder.
3.  Run: `npm init -y`
4.  Run: `npm install express twilio dotenv`
5.  Open `twilio-server.js` and paste your **Account SID** and **Auth Token**.
6.  Run: `node twilio-server.js`

## Phase 4: Connect App

1.  Your server is running at `http://localhost:3000`.
2.  For the Android app to reach it, use ngrok (free tool).
    *   Download [ngrok](https://ngrok.com/).
    *   Run `ngrok http 3000`.
    *   Copy the URL (e.g., `https://a1b2.ngrok.io`).
3.  Update `CallBridgeRepository.kt` in Android Studio:
    *   Set `isProduction = true`.
    *   Set the API URL to your ngrok URL.

## ✅ Done!
Now when you click "Call" in the app:
1.  App -> Your Server
2.  Your Server -> Twilio
3.  Twilio -> Calls User -> Calls Delivery Boy.
4.  **Both phones ring.**
