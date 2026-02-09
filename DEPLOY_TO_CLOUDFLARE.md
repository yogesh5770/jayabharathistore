# ⚡ Deploy to Cloudflare Workers (Fastest & Free)

Cloudflare Workers are better than Render because:
1.  **Instant Start:** No "sleep" delay. It wakes up in 0 milliseconds.
2.  **Generous Free Tier:** 100,000 requests per day (more than enough).
3.  **No Node.js needed:** I rewrote the code to use standard `fetch`, so it runs natively on the Edge.

### Step 1: Create Worker
1.  Go to [dash.cloudflare.com](https://dash.cloudflare.com/).
2.  Click **"Workers & Pages"** -> **"Create Application"**.
3.  Click **"Create Worker"**.
4.  Name it: `jayabharathi-call-bridge`.
5.  Click **"Deploy"** (don't worry about the code yet).

### Step 2: Paste Code
1.  Click **"Edit Code"**.
2.  Delete everything there.
3.  Copy-paste the code from the `worker.js` file I created in your project folder.
4.  Click **"Save and Deploy"**.

### Step 3: Add Secrets (IMPORTANT)
Cloudflare doesn't use `.env`. You add secrets in the dashboard.
1.  Go back to the Worker Settings.
2.  Click **"Settings"** -> **"Variables"**.
3.  Add these 3 variables:
    *   `TWILIO_ACCOUNT_SID` = `AC634...` (Your SID)
    *   `TWILIO_AUTH_TOKEN` = `59b9...` (Your Token)
    *   `TWILIO_PHONE_NUMBER` = `+12517665251` (Your Virtual Number)
4.  Click **"Save and Deploy"**.

### Step 4: Get URL
On the Worker overview page, you will see a URL like:
👉 `https://jayabharathi-call-bridge.your-name.workers.dev`

### Step 5: Update Android App
1.  Copy that URL.
2.  Paste it into `NetworkModule.kt` as the `BASE_URL`.
    *(Make sure to add a trailing slash `/` at the end!)*

### ✅ Done!
You now have a professional, enterprise-grade serverless backend running for free.
