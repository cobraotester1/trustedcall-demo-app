package com.example.myapp2

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.google.firebase.messaging.RemoteMessage
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.content.edit
import com.example.myapp2.old.CallConnection
import com.example.myapp2.old.CallConnectionService
import com.example.myapp2.telecom.TelecomCallService
import com.example.myapp2.utils.VLogUtils
import com.vonage.android_core.PushType
import com.vonage.jwt.Jwt
import java.time.ZonedDateTime
import com.vonage.android_core.VGClientInitConfig
import com.vonage.android_core.VGSessionImpl
import com.vonage.android_core.VGSessionListenerAPI
import com.vonage.clientcore.core.api.LoggingLevel
import com.vonage.clientcore.core.api.LegStatus
import com.vonage.clientcore.core.api.ClientConfigRegion
import com.vonage.clientcore.core.api.CallId

import com.vonage.clientcore.core.api.VoiceClientListener
//import com.vonage.clientcore.core.api.HangupReason
import com.vonage.voice.api.*

class  ClientManager(private val context: Context) {
    private var callInvite: CallId? = null
    private var session: String? = null
    private var call: CallId? = null
    private var token: String = ""

    private var client: VoiceClient
    private var telecomManager: TelecomManager
    private var phoneAccountHandle: PhoneAccountHandle

    companion object {
        // Volatile will guarantee a thread-safe & up-to-date version of the instance
        @Volatile
        private var instance: ClientManager? = null

        fun getInstance(context: Context): ClientManager {
            val funcName: String? = object {}.javaClass.enclosingMethod?.name
            VLogUtils.writeLogWithPrefix(this, funcName, "ENTER/EXIT function")
            return instance ?: synchronized(this) {
                instance ?: ClientManager(context).also { instance = it }
            }
        }
    }

    init {
        val funcName = "init"
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        val initConfig =
            VGClientInitConfig(LoggingLevel.Verbose, emptyList(), false, ClientConfigRegion.AP)
        initConfig.enableWebsocketInvites = true

        VLogUtils.writeLogWithPrefix(this, funcName, "JWT Token is $token")
        VLogUtils.writeLogWithPrefix(this, funcName, "Get Voice Client with context")
        client = VoiceClient(context)
        VLogUtils.writeLogWithPrefix(this, funcName, "Set configurations")
        client.setConfig(initConfig)
        VLogUtils.writeLogWithPrefix(this, funcName, "Set listeners")
        setListeners()

        VLogUtils.writeLogWithPrefix(this, funcName, "Get component name")
        val componentName = ComponentName(context, CallConnectionService::class.java)

        VLogUtils.writeLogWithPrefix(this, funcName, "Get telecom manager")
        telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        VLogUtils.writeLogWithPrefix(this, funcName, "Create a phone account handle")
        phoneAccountHandle = PhoneAccountHandle(componentName, "MyApp2 call Calling")

        VLogUtils.writeLogWithPrefix(this, funcName, "Set phone account capabilities")
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "MyApp2 Calling")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER).build()

        VLogUtils.writeLogWithPrefix(this, funcName, "Register phone account with telecom manager")
        telecomManager.registerPhoneAccount(phoneAccount)

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun generateJWTTokenForUser(userName: String): String {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        val applicationId = "e3531f0b-f2cc-4110-aad5-4252aada0c9c"
        val assetManager = context.assets
        val privateKeyContents: String = assetManager.open("private.key").bufferedReader().use {
            it.readText()
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "Create JWT token")
        val jwtToken: Jwt = Jwt.Builder()
            .applicationId(applicationId)
            .privateKeyContents(privateKeyContents)
            .subject(userName)
            .issuedAt(ZonedDateTime.now())
            .expiresAt(ZonedDateTime.now().plusMinutes(58))
            .addClaim(
                "acl", mapOf(
                    "paths" to mapOf(
                        "/*/users/**" to mapOf<String, Any>(),
                        "/*/conversations/**" to mapOf<String, Any>(),
                        "/*/rtc/**" to mapOf<String, Any>(),
                        "/*/sessions/**" to mapOf<String, Any>(),
                        "/*/devices/**" to mapOf<String, Any>(),
                        "/*/image/**" to mapOf<String, Any>(),
                        "/*/media/**" to mapOf<String, Any>(),
                        "/*/applications/**" to mapOf<String, Any>(),
                        "/*/push/**" to mapOf<String, Any>(),
                        "/*/knocking/**" to mapOf<String, Any>(),
                        "/*/legs/**" to mapOf<String, Any>()
                    )
                )
            ).build()

        return jwtToken.generate()
    }

    fun loginUser(userName: String, callback: ((String) -> Unit)? = null) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        VLogUtils.writeLogWithPrefix(this, funcName, "Create session")

        token = generateJWTTokenForUser(userName)
        client.createSession(token) { err, sessionId ->
            when {
                err != null -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Session could not be created")
                    callback?.invoke(err.localizedMessage)
                    VLogUtils.writeLogWithPrefix(this, funcName, "Message is ${err.localizedMessage}")
                }

                else -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Session created")
                    session = sessionId
                    callback?.invoke("Connected")
                }
            }
        }
    }

    private fun processPushCallInvite(remoteMessage: RemoteMessage) {
        // CSDemo: (3) Give the incoming push to the SDK to process
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        VLogUtils.writeLogWithPrefix(this, funcName, "Push call invite to client")
        client.processPushCallInvite(remoteMessage.data.toString())
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private fun startTelecomCallService(from: String) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Starting telecom call service")
        val intent = Intent(context, TelecomCallService::class.java)
        intent.setAction("incoming_call")
        //intent.putExtra("extra_name", from)
        intent.putExtra("extra_name", "ABC Company")
        intent.putExtra("extra_uri", Uri.parse("$from"))
        context.startService(intent)
    }

    private fun stopTelecomCallService() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Stopping telecom call service")
        val intent = Intent(context, TelecomCallService::class.java)
        context.stopService(intent)
    }

    private fun setListeners() {
        // CSDemo: (4) If the push is processed correctly, the setCallInviteListener is called.
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        VLogUtils.writeLogWithPrefix(this, funcName, "set call invite listener on client")
        client.setCallInviteListener { incomingCallId, from, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                callInvite = incomingCallId
                VLogUtils.writeLogWithPrefix(this, funcName, "Incoming Call ID is ..." + incomingCallId.toString())
                startTelecomCallService(from)
            } else {
                if (telecomManager.isIncomingCallPermitted(phoneAccountHandle)) {
                    callInvite = incomingCallId
                    val extras = Bundle()
                    extras.putString("from", from)

                    VLogUtils.writeLogWithPrefix(this, funcName, "Add new Incoming call to telecom manager")
                    // CSDemo: This calls the onCreateIncomingConnection function in the CallConnectionService class
                    telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
                }
            }
        }
        client.setOnCallHangupListener { _, _, _ ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Inside hangup listener")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Remote hangup detected")
                    stopTelecomCallService()
            }
        }
        client.setOnLegStatusUpdate  { callId, legId, status ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Inside setOnLegStatusUpdate")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (status == LegStatus.completed) {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Call completed")
                    stopTelecomCallService()
                }
            }
        }
        client.setCallInviteCancelListener { _, _ ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Call invite retracted")
            stopTelecomCallService()
        }
        client.setSessionErrorListener { _ ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Call closed")
            stopTelecomCallService()
        }
        client.setOnCallMediaErrorListener { _, _ ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Call closed")
            stopTelecomCallService()
        }
        client.setOnCallMediaDisconnectListener { _, _ ->
            VLogUtils.writeLogWithPrefix(this, funcName, "Call closed")
            stopTelecomCallService()
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun getStoredToken(): String? {
        return context.getSharedPreferences("vonage_pref", Context.MODE_PRIVATE)
            .getString("token", null)
    }

    fun registerToken(token: String) {
        // CSDemo: (0) For push to work, you need to register a token. This token maps this device to this user.
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        client.registerDevicePushToken(token) { err, deviceId ->
            if (err != null) {
                println("there was an error: $err")
                VLogUtils.writeLogWithPrefix(
                    this,
                    funcName,
                    "Client couldn't register device push token"
                )
            } else {
                context.getSharedPreferences("vonage_pref", Context.MODE_PRIVATE).edit {
                    putString("token", token)
                    putString("deviceId", deviceId)
                }
                println("registered device push token successfully - device id: $deviceId")
                VLogUtils.writeLogWithPrefix(this, funcName, "Client registered device push token")
            }
        }
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun startIncomingCall(remoteMessage: RemoteMessage) {
        /*
        CSDemo: (2) To process and answer a call, your user needs to be logged in.
        When the user is logged in, you can process the push. If the push processes
        correctly you will get a callInvite. This also checks for the correct push type
        and if the app has permission to use the system UI for calling.
        */
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        VLogUtils.writeLogWithPrefix(this, funcName, "Check if client is given permission to manage calls")
        if (context.checkSelfPermission(Manifest.permission.MANAGE_OWN_CALLS) == PackageManager.PERMISSION_GRANTED) {
            val type: PushType = VoiceClient.getPushNotificationType(remoteMessage.data.toString())

            when (type) {
                PushType.INCOMING_CALL -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Push type is Incoming call")
                    loginUser("userD") { status ->
                        if (status == "Connected") {
                            VLogUtils.writeLogWithPrefix(this, funcName, "Post Login, when status is connected, process push call invite")
                            processPushCallInvite(remoteMessage)
                        }
                    }
                }

                else -> {}
            }
        }
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun answerCall(callConnection: CallConnection) {
        // CSDemo: (7) The answer button has been pressed in the system UI. Answer the call with the SDK
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        callInvite?.let {
            VLogUtils.writeLogWithPrefix(this, funcName, "Make Client Answer")
            client.answer(it) { err ->
                VLogUtils.writeLogWithPrefix(this, funcName, "Within answer block")
                when {
                    err != null -> {
                        print("error answering call")
                        VLogUtils.writeLogWithPrefix(this, funcName, "Error answer call")
                    }

                    else -> {
                        call = it
                        VLogUtils.writeLogWithPrefix(this, funcName, "Setting call state to active")
                        callConnection.setActive()
                        VLogUtils.writeLogWithPrefix(
                            this,
                            funcName,
                            "Answered call, call status to active"
                        )
                        print("call answered")
                    }
                }
            }
        }
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun rejectCall(callConnection: CallConnection) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        callInvite?.let {
            VLogUtils.writeLogWithPrefix(this, funcName, "Ask client to Reject call")
            client.reject(it) { err ->
                when {
                    err != null -> {
                        VLogUtils.writeLogWithPrefix(this, funcName, "Error rejecting call")
                        print("error rejecting call")
                    }

                    else -> {
                        VLogUtils.writeLogWithPrefix(this, funcName, "Disconnect call")
                        callConnection.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                        callConnection.destroy()
                        VLogUtils.writeLogWithPrefix(
                            this,
                            funcName,
                            "Call disconnected and rejected"
                        )
                        print("call rejected")
                    }
                }
            }
        }
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun endCall(callConnection: CallConnection) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        call?.let {
            VLogUtils.writeLogWithPrefix(this, funcName, "Asking client to hangup call")
            client.hangup(it) { err ->
                when {
                    err != null -> {
                        print("error ending call")
                        VLogUtils.writeLogWithPrefix(this, funcName, "Failed to hangup call")
                    }

                    else -> {
                        print("call disconnected")
                        callConnection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                        callConnection.destroy()
                        VLogUtils.writeLogWithPrefix(
                            this,
                            funcName,
                            "Call hung up and disconnected"
                        )
                    }
                }
            }
        }
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    fun getCall(): CallId? {
        return call
    }

    fun getCallInvite(): CallId? {
        return callInvite
    }

    fun getClient(): VoiceClient {
        return client
    }

    fun getSession() : String? {
        return session
    }

    fun setCall(callId: CallId?) {
        call = callId
    }

    fun setCallInvite(callId: CallId?) {
        callInvite = callId
    }

    fun setClient(voiceClient: VoiceClient) {
        client = voiceClient
    }

    fun setSession(sessionId: String?) {
        session = sessionId
    }

}
