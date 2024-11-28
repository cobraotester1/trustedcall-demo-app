package com.example.myapp2.old

import android.net.Uri
import android.telecom.*
import com.example.myapp2.utils.VLogUtils

class CallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(connectionManagerPhoneAccount: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        /*
        CSDemo: (5) This gets the from number from the call invite in the ClientManager.
        A CallConnection Object is also created. This is how the system tells you the user has
        initiated an action such and answering/rejecting the call with the System UI.
         */
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
        val from = request?.extras?.getString("from")
        VLogUtils.writeLogWithPrefix(this, funcName, "Create new Connection")
        val connection = CallConnection(this)
        VLogUtils.writeLogWithPrefix(this, funcName, "Created new Connection")
        connection.setAddress(Uri.parse("tel:$from"), TelecomManager.PRESENTATION_ALLOWED)
        connection.connectionProperties =
            Connection.PROPERTY_SELF_MANAGED and
                    Connection.CAPABILITY_HOLD and
                    Connection.CAPABILITY_SUPPORT_HOLD and
                    Connection.CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION

        connection.setRinging()
        VLogUtils.writeLogWithPrefix(this, funcName, "Calling super")
        super.onCreateIncomingConnection(connectionManagerPhoneAccount, request)

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

}

