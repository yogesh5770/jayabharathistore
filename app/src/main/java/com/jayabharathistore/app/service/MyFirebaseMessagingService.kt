package com.jayabharathistore.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jayabharathistore.app.MainActivity
import com.jayabharathistore.app.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            val orderId = remoteMessage.data["orderId"]
            
            when (type) {
                "new_order", "assigned" -> showNewOrderNotification(orderId)
                "approved" -> showApprovedNotification()
                "order_update" -> showOrderStatusNotification(orderId, remoteMessage.data["status"])
            }
        }

        // Handle notification payload (if any) and if not handled by data
        remoteMessage.notification?.let {
            // Only show generic if we didn't handle it as a specialized data message
             if (!remoteMessage.data.containsKey("type")) {
                 showGenericNotification(it.title, it.body)
             }
        }
    }

    private fun showApprovedNotification() {
        val channelId = "delivery_approval_channel"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("route", "delivery_home") // Navigate to home
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Profile Approved!")
            .setContentText("You are now a registered Delivery Partner. Log in to start!")
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Delivery Approval",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for profile approval"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1001, notificationBuilder.build())
    }

    private fun showNewOrderNotification(orderId: String?) {
        val channelId = "delivery_orders_channel"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE) // Use Ringtone for "Phone Ringing" effect

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("route", "delivery_home") // Deep link to delivery home
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Ensure this resource exists
            .setContentTitle("New Order Received!")
            .setContentText("Tap to view order #${orderId?.takeLast(6)?.uppercase() ?: ""}")
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_MAX) // High priority for heads-up
            .setCategory(NotificationCompat.CATEGORY_CALL) // Treat like a call
            .setFullScreenIntent(pendingIntent, true) // Full screen intent for "Ringing" effect
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "New Delivery Orders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new delivery assignments"
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun showGenericNotification(title: String?, body: String?) {
        val channelId = "general_channel"
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun showOrderStatusNotification(orderId: String?, status: String?) {
        val channelId = "order_updates_channel"
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("route", "order_detail/$orderId")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, orderId.hashCode(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val readableStatus = when(status?.uppercase()) {
            "PACKED" -> "Packed and Ready"
            "OUT_FOR_DELIVERY" -> "Out for Delivery"
            "DELIVERED" -> "Delivered Successfully"
            "CANCELLED" -> "Cancelled"
            else -> status?.replace("_", " ") ?: "Updated"
        }

        val message = "Your order is now $readableStatus"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Order Update")
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for order status changes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(orderId.hashCode(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        // Send token to server if needed
    }
}
