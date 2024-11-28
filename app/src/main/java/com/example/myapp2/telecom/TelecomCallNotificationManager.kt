package com.example.myapp2.telecom

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
//import androidx.core.graphics.drawable.IconCompat
import androidx.core.content.PermissionChecker
import com.example.myapp2.R
import com.example.myapp2.utils.VLogUtils


/**
 * Handles call status changes and updates the notification accordingly. For more guidance around
 * notifications check https://developer.android.com/develop/ui/views/notifications
 *
 * @see updateCallNotification
 */
//@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallNotificationManager(private val context: Context) {

    internal companion object {
        const val TELECOM_NOTIFICATION_ID = 200
        const val TELECOM_NOTIFICATION_ACTION = "telecom_action"
        const val TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID = "telecom_incoming_channel"
        const val TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID = "telecom_ongoing_channel"

        private val ringToneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    /**
     * Updates, creates or dismisses a CallStyle notification based on the given [TelecomCall]
     */
    fun updateCallNotification(call: TelecomCall) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // If notifications are not granted, skip it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            return
        }

        // Ensure that the channel is created
        createNotificationChannels()

        // Update or dismiss notification
        when (call) {
            TelecomCall.None, is TelecomCall.Unregistered -> {
                notificationManager.cancel(TELECOM_NOTIFICATION_ID)
            }

            is TelecomCall.Registered -> {
                /// Vonage Code
                notificationManager.cancel(TELECOM_NOTIFICATION_ID)
                ///
                val notification = createNotification(call)
                notificationManager.notify(TELECOM_NOTIFICATION_ID, notification)
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private fun createNotification(call: TelecomCall.Registered): Notification {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // To display the caller information
        val caller = Person.Builder()
            .setName(call.callAttributes.displayName)
            .setUri(call.callAttributes.address.toString())
            .setImportant(true)
            .build()

        // Defines the full screen notification activity or the activity to launch once the user taps
        // on the notification
        val contentIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ Intent(context, TelecomCallActivity::class.java),
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Define the call style based on the call state and set the right actions
        val isIncoming = call.isIncoming() && !call.isActive

        /*
        var answerAction: NotificationCompat.Action? = null
        if (isIncoming) {
            answerAction =
                NotificationCompat.Action.Builder(
                IconCompat.createWithResource(context, R.drawable.call_24px),
                "accept",
                getPendingIntent(TelecomCallAction.Answer)
                ).build()
        }
        */

        val callStyle = if (isIncoming) {

            NotificationCompat.CallStyle.forIncomingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.REJECTED),
                    ),
                ),
                getPendingIntent(TelecomCallAction.Answer),
            )
             /*
            NotificationCompat.CallStyle.forScreeningCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.REJECTED),
                    ),
                ),
                getPendingIntent(TelecomCallAction.Answer),
            )*/
        } else {
            NotificationCompat.CallStyle.forOngoingCall(
                caller,
                getPendingIntent(
                    TelecomCallAction.Disconnect(
                        DisconnectCause(DisconnectCause.LOCAL),
                    ),
                ),
            )
        }

        callStyle.setVerificationText("Branded Call")


        val channelId = if (isIncoming) {
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID
        } else {
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .setSmallIcon(R.drawable.call_24px)
            .setOngoing(true)
            .setStyle(callStyle)
            .setAutoCancel(true)

        // TODO figure out why custom actions are not working
        if (call.isOnHold) {
            builder.addAction(
                R.drawable.phone_paused_24px, "Resume",
                getPendingIntent(
                    TelecomCallAction.Activate,
                ),
            )
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")

        return builder.build()
    }

    /**
     * Creates a PendingIntent for the given [TelecomCallAction]. Since the actions are parcelable
     * we can directly pass them as extra parameters in the bundle.
     */
    private fun getPendingIntent(action: TelecomCallAction): PendingIntent {
        val callIntent = Intent(context, TelecomCallBroadcast::class.java)
        callIntent.putExtra(
            TELECOM_NOTIFICATION_ACTION,
            action,
        )

        return PendingIntent.getBroadcast(
            context,
            callIntent.hashCode(),
            callIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannels() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        val incomingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_INCOMING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        ).setName("Incoming calls")
            .setDescription("Handles the notifications when receiving a call")
            .setVibrationEnabled(true).setSound(
                ringToneUri,
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build(),
            ).build()

        val ongoingChannel = NotificationChannelCompat.Builder(
            TELECOM_NOTIFICATION_ONGOING_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT,
        ).setName("Ongoing calls").setDescription("Displays the ongoing call notifications").build()

        notificationManager.createNotificationChannelsCompat(
            listOf(
                incomingChannel,
                ongoingChannel,
            ),
        )

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }
}

