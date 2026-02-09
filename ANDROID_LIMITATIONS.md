# 🚫 Technical Limitation: Hiding Numbers in Android Dialer

## The Core Problem
You want to **hide the real number** in the Android Native Dialer (the green phone app) **without** using a Cloud Bridge (Twilio).

**This is impossible.**

### Why?
1.  **Android Security**: Apps cannot change what the Native Dialer shows. If you tell Android to "Dial 9876543210", the Dialer **WILL** show "Calling 9876543210".
2.  **No Overlay Permission**: Modern Android (10+) blocks apps from drawing over the active call screen for security reasons (to prevent spam/fraud).

## The Only Workarounds (Without Cloud Server)

### Workaround 1: Contact Name Injection (The "TrueCaller" Trick)
Instead of dialing the raw number, we can:
1.  Programmatically **save** the Delivery Boy's number as a Contact named "Delivery Partner (Secure)".
2.  Dial that **Contact**.
3.  The Dialer will show **"Delivery Partner (Secure)"** instead of the raw number (though the number is still visible in small text).
4.  Delete the contact after the call.

### Workaround 2: The "Fake" Dialer (App-Internal)
We build a screen that *looks* like a call screen inside our app.
*   **Pros**: We control the UI completely. We can show "Calling...".
*   **Cons**: It's fake. It doesn't actually connect to the telecom network. It just sits there.

## Recommended Path: Workaround 1 (Contact Injection)
This is the closest you can get to "hiding" the number without a backend. It makes the **Name** prominent.

I will implement Workaround 1 now.
