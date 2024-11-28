package com.example.myapp2.old

import android.content.Context
import android.telecom.Connection
import com.example.myapp2.ClientManager
import com.example.myapp2.utils.VLogUtils

class CallConnection(
    context: Context
) : Connection() {

    private var clientManager = ClientManager.getInstance(context)

    init {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Setting audio mode voip")
        audioModeIsVoip = true
    }

    // CSDemo: (6) Here you will get events back from the system UI.
    override fun onDisconnect() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Ending call")
        clientManager.endCall(this)
        VLogUtils.writeLogWithPrefix(this, funcName, "Call ended")
        super.onDisconnect()
    }

    override fun onAnswer() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Answering call")
        clientManager.answerCall(this)
        VLogUtils.writeLogWithPrefix(this, funcName, "Call answered")
    }

    override fun onReject() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Rejecting call")
        clientManager.rejectCall(this)
        VLogUtils.writeLogWithPrefix(this, funcName, "Call rejected")
    }

    override fun onShowIncomingCallUi() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER/EXIT function")
    //    this.setRinging();
    }

    override fun onHold() {
        this.setOnHold()
    }

    override fun onUnhold() {
        this.setActive()
    }
}