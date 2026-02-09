package com.jayabharathistore.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jayabharathistore.app.R

class AppFirebaseMessagingService : FirebaseMessagingService() {
    private val CHANNEL_ID = "jb_orders_channel"

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "New Order"
        val body = message.notification?.body ?: data["body"] ?: ""
        val event = data["event"] ?: ""

        // Play a short beep for order-related events (delivery devices subscribe to delivery topic)
        if (event == "ORDER_PLACED" || event == "ORDER_PLACED_PAID" || event == "PAYMENT_SUCCESS") {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            } catch (_: Exception) { }
        }

        // Show a notification so user sees it
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Orders", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        nm.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }
}


