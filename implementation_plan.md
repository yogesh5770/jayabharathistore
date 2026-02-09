# Implementation Plan - Advanced Delivery & Payment System

## 1. Foundation & Configuration
- [ ] Add Cashfree SDK dependencies to `app/build.gradle.kts`.
- [ ] Update `AndroidManifest.xml` with necessary services for location tracking and Cashfree.
- [ ] Configure `google-services.json` (already present, but verify keys).
- [ ] Define shared constants for Order Statuses: `PENDING`, `ACCEPTED`, `PACKING`, `OUT_FOR_DELIVERY`, `REACHED`, `COMPLETED`.

## 2. Store Status & Availability (Owner & Customer)
- [ ] Create `ShopStatusRepository` to manage store open/closed state.
- [ ] Add toggle in `StoreHomeScreen` for Owner to open/close store.
- [ ] Implement global check in `CartScreen` to prevent checkout if store is closed.
- [ ] Add "Store Closed" banner/message in `HomeScreen`.

## 3. Checkout & Payment Integration
- [ ] Implement `PaymentRepository` using Cashfree SDK.
- [ ] Update `CheckoutScreen` (create if not exists or update current one) to handle:
    - [ ] Payment method selection (UPI, Card, Wallet, COD).
    - [ ] Cashfree SDK trigger for online payments.
    - [ ] Success/Failure redirection.
- [ ] Create a "Payment Success" animation screen with a green tick and order details.

## 4. Delivery Partner Application (Flavor: Delivery)
- [ ] Create `DeliveryHomeScreen` for the delivery app.
- [ ] Implement hardcoded login check for `DELIVERY@JBSTORE.com` / `Delivery@js123`.
- [ ] Enable "Beep Sound" notification for new orders using Firebase Cloud Messaging (FCM) or a local listener.
- [ ] Implement "Order List" for delivery personnel.
- [ ] Implement "Accept Order" logic (moves status from `PENDING` to `ACCEPTED`).
- [ ] Implement Status Transition: `ACCEPTED` -> `PACKING` (Owner) -> `OUT_FOR_DELIVERY` (Delivery) -> `REACHED` (Delivery).
- [ ] Add "I am Reached" slider for status update.
- [ ] Add "Call Customer" functionality.

## 5. Live Tracking & Maps
- [ ] Implement `LocationService` for delivery partners to stream their coordinates to Firestore.
- [ ] Add Google Maps integration in `OrderTrackingScreen` (Customer side):
    - [ ] Small map popup expandable to full screen.
    - [ ] Marker for "Jayabharathi Store".
    - [ ] Real-time marker for delivery partner location.
- [ ] Add "Open in Google Maps" button for Delivery partner with customer's location.

## 6. Owner Dashboard Refactor
- [ ] Remove "Accept Order" button from `StoreHomeScreen`.
- [ ] Update UI to only show order details, status, and earnings.
- [ ] Sync with Delivery app status updates.

## 7. UI/UX Polish
- [ ] Ensure "Industry Level" design using modern color palettes (PrimaryPurple, AccentOrange).
- [ ] Add micro-animations for status changes.
- [ ] Optimize database listeners for real-time performance.
