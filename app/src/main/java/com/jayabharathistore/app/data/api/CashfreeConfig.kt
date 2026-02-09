package com.jayabharathistore.app.data.api

/**
 * Cashfree Payment Gateway Configuration
 * 
 * IMPORTANT: In production, these keys should be stored securely:
 * - Use BuildConfig for API keys (configured in build.gradle)
 * - Or fetch session tokens from your secure backend server
 * - Never commit actual keys to version control
 */
object CashfreeConfig {
    // Test Mode Credentials
    const val APP_ID = "TEST109807064097ob23977bea08c66660708901"
    
    // Environment
    const val IS_PRODUCTION = false
    
    /**
     * In a real production app, you would:
     * 1. Send order details to your backend server
     * 2. Backend generates order session using Cashfree's Create Order API
     * 3. Backend returns the session token to the app
     * 4. App uses the session token to initiate payment
     * 
     * This ensures your SECRET_KEY is never exposed in the app.
     */
}
