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

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import com.example.myapp2.ClientManager
import com.example.myapp2.utils.VLogUtils
//import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The central repository that keeps track of the current call and allows to register new calls.
 *
 * This class contains the main logic to integrate with Telecom SDK.
 *
 * @see registerCall
 */
//@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallRepository(private val callsManager: CallsManager) {

    companion object {
        //// Vonage Code
        var clientManager: ClientManager? = null
        ////
        var instance: TelecomCallRepository? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create a singleton with the CallsManager class.
         */
        fun create(context: Context): TelecomCallRepository {
            val funcName: String? = object {}.javaClass.enclosingMethod?.name
            VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

            Log.d("MPB", "New instance")
            check(instance == null) {
                "CallRepository instance already created"
            }

            //// Vonage Code
            println("getting the client manager instance")
            clientManager = ClientManager.getInstance(context)
            ////

            // Create the Jetpack Telecom entry point
            val callsManager = CallsManager(context).apply {
                // Register with the telecom interface with the supported capabilities
                registerAppWithTelecom(
                    capabilities = CallsManager.CAPABILITY_SUPPORTS_CALL_STREAMING and
                            CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING,
                )
            }

            return TelecomCallRepository(
                callsManager = callsManager,
            ).also {
                instance = it
            }

            VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
        }
    }

    // Keeps track of the current TelecomCall state
    private val _currentCall: MutableStateFlow<TelecomCall> = MutableStateFlow(TelecomCall.None)
    val currentCall = _currentCall.asStateFlow()

   /*
    suspend fun disconnectCall() {
        doDisconnect(TelecomCallAction.Disconnect(DisconnectCause(DisconnectCause.REMOTE)))
        _currentCall.value = TelecomCall.None
    }
*/
    /**
     * Register a new call with the provided attributes.
     * Use the [currentCall] StateFlow to receive status updates and process call related actions.
     */
    suspend fun registerCall(displayName: String, address: Uri, isIncoming: Boolean) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // For simplicity we don't support multiple calls
        check(_currentCall.value !is TelecomCall.Registered) {
            "There cannot be more than one call at the same time."
        }

        // Create the call attributes
        val attributes = CallAttributesCompat(
            displayName = displayName,
            address = address,
            direction = if (isIncoming) {
                CallAttributesCompat.DIRECTION_INCOMING
            } else {
                CallAttributesCompat.DIRECTION_OUTGOING
            },
            callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
            callCapabilities = (CallAttributesCompat.SUPPORTS_SET_INACTIVE
                    or CallAttributesCompat.SUPPORTS_STREAM
                    or CallAttributesCompat.SUPPORTS_TRANSFER),
        )

        // Creates a channel to send actions to the call scope.
        val actionSource = Channel<TelecomCallAction>()
        // Register the call and handle actions in the scope
        try {
            VLogUtils.writeLogWithPrefix(this, funcName, "Invoking addcall() to call manager")
            callsManager.addCall(
                attributes,
                onIsCallAnswered, // Watch needs to know if it can answer the call
                onIsCallDisconnected,
                onIsCallActive,
                onIsCallInactive
            ) {
                // Consume the actions to interact with the call inside the scope
                launch {
                    VLogUtils.writeLogWithPrefix(this, funcName, "calling processcallactions")
                    processCallActions(actionSource.consumeAsFlow())
                }

                VLogUtils.writeLogWithPrefix(this, funcName, "Registering current call to call id ${getCallId()} ...")
                // Update the state to registered with default values while waiting for Telecom updates
                _currentCall.value = TelecomCall.Registered(
                    id = getCallId(),
                    isActive = false,
                    isOnHold = false,
                    callAttributes = attributes,
                    isMuted = false,
                    errorCode = null,
                    currentCallEndpoint = null,
                    availableCallEndpoints = emptyList(),
                    actionSource = actionSource,
                )

                launch {
                    currentCallEndpoint.collect {
                        updateCurrentCall {
                            copy(currentCallEndpoint = it)
                        }
                    }
                }
                launch {
                    availableEndpoints.collect {
                        updateCurrentCall {
                            copy(availableCallEndpoints = it)
                        }
                    }
                }
                launch {
                    isMuted.collect {
                        updateCurrentCall {
                            copy(isMuted = it)
                        }
                    }
                }
            }
        } finally {
            _currentCall.value = TelecomCall.None
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     * Collect the action source to handle client actions inside the call scope
     */
    private suspend fun CallControlScope.processCallActions(actionSource: Flow<TelecomCallAction>) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        actionSource.collect { action ->
            when (action) {
                is TelecomCallAction.Answer -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "calling doAnswer")
                    doAnswer()
                }

                is TelecomCallAction.Disconnect -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "calling doDisconnect")
                    doDisconnect(action)
                }

                is TelecomCallAction.SwitchAudioEndpoint -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "calling doSwitchEndpoint")
                    doSwitchEndpoint(action)
                }

                is TelecomCallAction.TransferCall -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "transferring call..")
                    val call = _currentCall.value as? TelecomCall.Registered
                    val endpoints = call?.availableCallEndpoints?.firstOrNull {
                        it.identifier == action.endpointId
                    }
                    requestEndpointChange(
                        endpoint = endpoints ?: return@collect,
                    )
                }

                TelecomCallAction.Hold -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "action is to hold")
                    when (val result = setInactive()) {
                        is CallControlResult.Success -> {
                            VLogUtils.writeLogWithPrefix(this, funcName, "setinactive success, calling onIsCallInactive")
                            onIsCallInactive()
                        }

                        is CallControlResult.Error -> {
                            VLogUtils.writeLogWithPrefix(this, funcName, "setinactive failed")
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                TelecomCallAction.Activate -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "action is setactive")
                    when (val result = setActive()) {
                        is CallControlResult.Success -> {
                            VLogUtils.writeLogWithPrefix(this, funcName, "setactive success, calling onIsCallActive")
                            onIsCallActive()
                        }

                        is CallControlResult.Error -> {
                            VLogUtils.writeLogWithPrefix(this, funcName, "setactive failed")
                            updateCurrentCall {
                                copy(errorCode = result.errorCode)
                            }
                        }
                    }
                }

                is TelecomCallAction.ToggleMute -> {
                    VLogUtils.writeLogWithPrefix(this, funcName, "action is mute")
                    // We cannot programmatically mute the telecom stack. Instead we just update
                    // the state of the call and this will start/stop audio capturing.
                    updateCurrentCall {
                        copy(isMuted = !isMuted)
                    }
                }
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     * Update the current state of our call applying the transform lambda only if the call is
     * registered. Otherwise keep the current state
     */
    private fun updateCurrentCall(transform: TelecomCall.Registered.() -> TelecomCall) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        _currentCall.update { call ->
            if (call is TelecomCall.Registered) {
                call.transform()
            } else {
                call
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private suspend fun CallControlScope.doSwitchEndpoint(action: TelecomCallAction.SwitchAudioEndpoint) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        // TODO once availableCallEndpoints is a state flow we can just get the value
        val endpoints = (_currentCall.value as TelecomCall.Registered).availableCallEndpoints

        // Switch to the given endpoint or fallback to the best possible one.
        val newEndpoint = endpoints.firstOrNull { it.identifier == action.endpointId }

        if (newEndpoint != null) {
            requestEndpointChange(newEndpoint).also {
                Log.d("MPB", "Endpoint ${newEndpoint.name} changed: $it")
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private suspend fun CallControlScope.doDisconnect(action: TelecomCallAction.Disconnect) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        disconnect(action.cause)
        onIsCallDisconnected(action.cause)

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private suspend fun CallControlScope.doAnswer() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        when (answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
            is CallControlResult.Success -> {
                onIsCallAnswered(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)
            }

            is CallControlResult.Error -> {
                updateCurrentCall {
                    TelecomCall.Unregistered(
                        id = id,
                        callAttributes = callAttributes,
                        disconnectCause = DisconnectCause(DisconnectCause.BUSY),
                    )
                }
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     *  Can the call be successfully answered??
     *  TIP: We would check the connection/call state to see if we can answer a call
     *  Example you may need to wait for another call to hold.
     **/
    val onIsCallAnswered: suspend(type: Int) -> Unit = {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        //// Vonage Code
        if (clientManager==null) {
            println("client manager is null")
        } else {
            println("client manager is NOT null")
        }
        val callInvite = clientManager?.getCallInvite()
        val client = clientManager?.getClient()
        callInvite.let {
            println("#### placing a call to Vonage client with call id $it")
            client?.answer(it ?:""){ err ->
                when {
                    err != null -> {
                        println("error answering the call")
                    }

                    else -> {
                        clientManager?.setCall(it)
                    }
                }
            }
        }
        ////
        updateCurrentCall {
            copy(isActive = true, isOnHold = false)
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     * Can the call perform a disconnect
     */
    val onIsCallDisconnected: suspend (cause: DisconnectCause) -> Unit = {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        //// Vonage Code
        val callId = clientManager?.getCall()
        val callInvite = clientManager?.getCallInvite()
        val client = clientManager?.getClient()

        when (it.getCode()) {
            DisconnectCause.REJECTED ->
                callInvite?.let {
                    println("#### rejecting a call to Vonage client with call id $it")
                    client?.reject(it ?: "")
                }
            else -> {
                callId?.let {
                    println("#### disconnecting a call to Vonage client with call id $it")
                    client?.hangup(it ?: "")
                }
            }
        }
        ////
        updateCurrentCall {
            TelecomCall.Unregistered(id, callAttributes, it)
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     *  Check is see if we can make the call active.
     *  Other calls and state might stop us from activating the call
     */
    val onIsCallActive: suspend () -> Unit = {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        updateCurrentCall {
            copy(
                errorCode = null,
                isActive = true,
                isOnHold = false,
            )
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    /**
     * Check to see if we can make the call inactivate
     */
    val onIsCallInactive: suspend () -> Unit = {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        updateCurrentCall {
            copy(
                errorCode = null,
                isOnHold = true)
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }
}