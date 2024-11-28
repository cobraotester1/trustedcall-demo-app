package com.example.myapp2

import com.example.myapp2.utils.VLogUtils
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService: FirebaseMessagingService() {

    companion object {
        fun requestToken(onSuccessCallback: ((String) -> Unit)? = null) {
            val funcName: String? = object {}.javaClass.enclosingMethod?.name
            VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

            VLogUtils.writeLogWithPrefix(this, funcName, "Add OnComplete Firebase listener")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Firebase instance exists")
                    task.result?.let { token ->
                        onSuccessCallback?.invoke(token)
                        VLogUtils.writeLogWithPrefix(this, funcName, "Firebase listener added")
                    }
                }
            }
            VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
        }
    }

    override fun onNewToken(token: String) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        super.onNewToken(token)
        ClientManager.getInstance(applicationContext).registerToken(token)
        VLogUtils.writeLogWithPrefix(this, funcName, "new firebase token registered")
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        //wakeApp()
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        // CSDemo: (1) When an incoming push notification comes in, notify the ClientManager
        VLogUtils.writeLogWithPrefix(this, funcName, "Start an incoming call")
        ClientManager.getInstance(applicationContext).startIncomingCall(remoteMessage)
        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }
}