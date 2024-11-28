package com.example.myapp2.old

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.myapp2.R

interface INotification {
    fun createNotification()
}

class NotificationManagerImpl(private val context: Context) : INotification {

    fun createNotificationChannel(): NotificationManager {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Notification Channel"
            val descriptionText = "This is my custom notification channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("myChannelId", name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        return notificationManager
    }

    override fun createNotification() {

        val view = RemoteViews(context.packageName, R.layout.notification)
        // Create expanded view if required
        val expandedView = RemoteViews(context.packageName, R.layout.notification)
        view.setTextViewText(R.id.title, "Custom Title")

        expandedView.setTextViewText(R.id.title, "Custom Title")
        expandedView.setTextViewText(R.id.message, "Custom Message")

        val notificationBuilder = NotificationCompat.Builder(context, "myChannelId")
            // Custom Decoration for Notification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            // Mandatory fields
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("My Custom Notification")
            .setContentText("This is a custom notification in Kotlin!")
            // end of mandatory fields

            // setting the custom collapsed and expanded views
            .setCustomContentView(view)
            .setCustomBigContentView(expandedView)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = createNotificationChannel()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            println("######################### Logging 20 ##############")
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(1, notificationBuilder.build())
    }
}