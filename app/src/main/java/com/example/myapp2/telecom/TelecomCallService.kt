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
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.DisconnectCause
import androidx.core.content.PermissionChecker
//import androidx.core.telecom.CallsManager
import com.example.myapp2.utils.VLogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


/**
 * This service handles the app call logic (show notification, record mic, display audio, etc..).
 * It can get started by the user or by an upcoming push notification to start a call.
 *
 * It holds the call scope used to register a call with the Telecom SDK in our TelecomCallRepository.
 *
 * When registering a call with the Telecom SDK and displaying a CallStyle notification, the SDK will
 * grant you foreground service delegation so there is no need to make this a FGS.
 *
 * Note: you could potentially make this service run in a different process since audio or video
 * calls can consume significant memory, although that would require more complex setup to make it
 * work across multiple process.
 */
//@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallService : Service() {

    companion object {
        internal const val EXTRA_NAME: String = "extra_name"
        internal const val EXTRA_URI: String = "extra_uri"
        internal const val ACTION_INCOMING_CALL = "incoming_call"
        internal const val ACTION_OUTGOING_CALL = "outgoing_call"
        internal const val ACTION_UPDATE_CALL = "update_call"
    }

    private lateinit var notificationManager: TelecomCallNotificationManager
    private lateinit var telecomRepository: TelecomCallRepository

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
   //private var audioJob: Job? = null

    override fun onCreate() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        super.onCreate()

        println("##### starting notification manager and telecom repository ####")
        notificationManager = TelecomCallNotificationManager(applicationContext)
        telecomRepository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create(applicationContext)

        // Observe call status updates once the call is registered and update the service
        telecomRepository.currentCall
            .onEach { call ->
                updateServiceState(call)
            }
            .onCompletion {
                // If the scope is completed stop the service
                stopSelf()
            }
            .launchIn(scope)

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    override fun onDestroy() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        super.onDestroy()
        // Remove notification and clean resources
        /// Vonage code
        scope.launch {
            (telecomRepository.currentCall.value as? TelecomCall.Registered)?.processAction(
                TelecomCallAction.Disconnect(DisconnectCause(DisconnectCause.REMOTE)),
            )
        }
        ///
        scope.cancel()
        notificationManager.updateCallNotification(TelecomCall.None)

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        if (intent == null) {
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_INCOMING_CALL -> registerCall(intent = intent, incoming = true)
            ACTION_OUTGOING_CALL -> registerCall(intent = intent, incoming = false)
            ACTION_UPDATE_CALL -> updateServiceState(telecomRepository.currentCall.value)

            else -> throw IllegalArgumentException("Unknown action")
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")

        return START_STICKY
    }

    private fun registerCall(intent: Intent, incoming: Boolean) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // If we have an ongoing call ignore command
        if (telecomRepository.currentCall.value is TelecomCall.Registered) {
            VLogUtils.writeLogWithPrefix(this, funcName, "call already exists")
            return
        }

        val name = intent.getStringExtra(EXTRA_NAME)!!
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_URI)!!
        }

        scope.launch {
            if (incoming) {
                // fake a delay for the incoming call for demo purposes
                //delay(2000)
            }

            launch {
                // Register the call with the Telecom stack
                VLogUtils.writeLogWithPrefix(this, funcName, "registering call with repository with name $name uri $uri ....")
                telecomRepository.registerCall(name, uri, incoming)
            }

            if (!incoming) {
                // If doing an outgoing call, fake the other end picks it up for demo purposes.
                //delay(2000)
                (telecomRepository.currentCall.value as? TelecomCall.Registered)?.processAction(
                    TelecomCallAction.Activate,
                )
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     * Update our calling service based on the call state. Here is where you would update the
     * connection socket, the notification, etc...
     */
    @SuppressLint("MissingPermission")
    private fun updateServiceState(call: TelecomCall) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // Update the call notification
        notificationManager.updateCallNotification(call)

        when (call) {
            is TelecomCall.None -> {
                // Stop any call tasks, in this demo we stop the audio loop
                //audioJob?.cancel()
            }

            is TelecomCall.Registered -> {
                // Update the call state.
                // For this sample it means start/stop the audio loop
                if (call.isActive && !call.isOnHold && !call.isMuted && hasMicPermission()) {
                   // if (audioJob == null || audioJob?.isActive == false) {
                     //   audioJob = scope.launch {
                           // AudioLoopSource.openAudioLoop()
                       // }
                   // }
                } else {
                    //audioJob?.cancel()
                }
            }

            is TelecomCall.Unregistered -> {
                // Stop service and clean resources
                stopSelf()
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun hasMicPermission() =
        PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PermissionChecker.PERMISSION_GRANTED

}