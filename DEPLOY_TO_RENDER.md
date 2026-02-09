# 🌐 How to Host Your Twilio Server 24/7 (Free)

For a server that runs **24/7** without your laptop, **Render.com** is the best free option.

### Step 1: Prepare Your Code (I have done this)
You already have:
1.  `package.json` (defines dependencies)
2.  `twilio-server.js` (your server code)

### Step 2: Push to GitHub
You need to put this project on GitHub.
1.  Create a new Repository on GitHub (e.g., `twilio-backend`).
2.  Upload just these 2 files: `package.json` and `twilio-server.js`.

### Step 3: Deploy on Render (5 Minutes)
1.  Go to [dashboard.render.com](https://dashboard.render.com/).
2.  Click **"New +"** -> **"Web Service"**.
3.  Connect your GitHub account and select your `twilio-backend` repo.
4.  **Settings:**
    *   **Name:** `jayabharathi-call-server`
    *   **Runtime:** Node
    *   **Build Command:** `npm install`
    *   **Start Command:** `node twilio-server.js`
    *   **Instance Type:** Free
5.  Click **"Create Web Service"**.

### Step 4: Get Your URL
Render will take 1-2 minutes to build.
Once done, it will give you a URL like:
👉 `https://jayabharathi-call-server.onrender.com`

### Step 5: Update Android App
1.  Copy that URL.
2.  Open `NetworkModule.kt` in Android Studio.
3.  Replace the `BASE_URL` with your new Render URL.

### ✅ Done!
Your server now runs **24/7** in the cloud. You can turn off your laptop, and the app will still work.
