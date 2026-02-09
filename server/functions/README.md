Firebase Functions (Cashfree) — quick start

1) Setup
- Install firebase-tools and login:
  npm install -g firebase-tools
  firebase login

2) Install dependencies
  cd server/functions
  npm install

3) Configure Cashfree secrets (use firebase functions:config:set)
  firebase functions:config:set cashfree.client_id="YOUR_CLIENT_ID" cashfree.secret="YOUR_SECRET" cashfree.env="sandbox"

4) Local testing (emulator)
  npm run serve

5) Deploy
  npm run build
  npm run deploy

Endpoints
- POST /createOrder : create order, auto-mark zero-amount orders as PAID, creates Cashfree order token if credentials present.
- POST /webhook/cashfree : Cashfree webhook (raw body required).
- GET /orders/{orderId}/verify : fetch order document for verification by client.

Notes
- For production, protect endpoints and validate authenticated user tokens.
- Implement notification triggers (FCM) in webhook/createOrder path to notify store/delivery apps.


