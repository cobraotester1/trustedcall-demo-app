package com.example.myapp2

// Importing others
import android.Manifest
//import android.content.ContextWrapper
//import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import android.widget.Toast
import android.os.Build
import android.util.Log
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.LinearLayout
import com.example.myapp2.utils.VLogUtils
import com.google.android.material.textfield.TextInputEditText

//import android.annotation.SuppressLint

class MainActivity : AppCompatActivity() {
    private var otherUser: String = ""
    private var callContextVal: String = ""

    private lateinit var clientManager: ClientManager
    private val PERMISSIONS_REQUEST_CODE: Int = 123
    private lateinit var callContextLayout: LinearLayout
    private lateinit var connectionStatusTextView: TextView
    private lateinit var waitingForIncomingCallTextView: TextView
    private lateinit var callContextInputText: EditText
    private lateinit var callContextSubmitButton: Button
    private lateinit var loginAsUserC: Button
    private lateinit var loginAsUserD: Button
    private lateinit var startCallButton: Button
    private lateinit var endCallButton: Button
    //private lateinit var telecomButton: Button

    private var requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.d("POST_NOTIFICATION_PERMISSION", "USER DENIED PERMISSION")
            } else {
                Log.d("POST_NOTIFICATION_PERMISSION", "USER GRANTED PERMISSION")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        super.onCreate(savedInstanceState)

        clientManager = ClientManager.getInstance(this.application.applicationContext)

        VLogUtils.writeLogWithPrefix(this, funcName, "Call setContentView")
        setContentView(R.layout.activity_main)

        VLogUtils.writeLogWithPrefix(this, funcName, "Get Client Instance")

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        // request permission
        permissions.add(Manifest.permission.MANAGE_OWN_CALLS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
            ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_OWN_CALLS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            VLogUtils.writeLogWithPrefix(this, funcName, "Request Audio Permission")
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "Request Notification Permission")
        requestNotificationPermission()

        callContextLayout = findViewById(R.id.contextLayout)
        callContextInputText = findViewById(R.id.contextInputTextBox)
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)
        waitingForIncomingCallTextView = findViewById(R.id.waitingForIncomingCallTextView)
        loginAsUserC = findViewById(R.id.loginAsUserC)
        loginAsUserD = findViewById(R.id.loginAsUserD)
        callContextSubmitButton = findViewById(R.id.contextSubmitButton)
        startCallButton = findViewById(R.id.startCallButton)
        endCallButton = findViewById(R.id.endCallButton)

        callContextSubmitButton.setOnClickListener { assignCallContext() }
        loginAsUserC.setOnClickListener { loginAsUserC() }
        loginAsUserD.setOnClickListener { loginFirebaseMessaging() }
        startCallButton.setOnClickListener { startCall() }
        endCallButton.setOnClickListener { endCall() }

        //telecomButton = findViewById(R.id.telecomButton)
        //telecomButton.setOnClickListener { enableTelecomPermission() }

    }

    private fun hideUI() {
        val content = findViewById<LinearLayout>(R.id.content)
        content.forEach { it.visibility = View.GONE }
    }

    private fun loginFirebaseMessaging() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "Client Manager Start Login")

        clientManager.loginUser("userD") { status ->
            runOnUiThread {
                VLogUtils.writeLogWithPrefix(this, funcName, "check if status is connected")
                if (status == "Connected") {
                    VLogUtils.writeLogWithPrefix(this, funcName, "status is connected")
                    //telecomButton.visibility = VISIBLE
                    hideUI()
                    connectionStatusTextView.visibility = View.VISIBLE
                    connectionStatusTextView.text = "Connected"
                    startCallButton.visibility = View.GONE
                    waitingForIncomingCallTextView.visibility = View.VISIBLE

                    VLogUtils.writeLogWithPrefix(this, funcName, "Request Firebase Token")
                    FirebaseMessagingService.requestToken { token ->
                        val storedToken = clientManager.getStoredToken()
                        if (token != storedToken) {
                            VLogUtils.writeLogWithPrefix(
                                this,
                                funcName,
                                "Client Manager register firebase token"
                            )
                            clientManager.registerToken(token)
                        }
                    }
                } else {
                    VLogUtils.writeLogWithPrefix(this, funcName, "Client status is not connected")
                    hideUI()
                    connectionStatusTextView.visibility = View.VISIBLE
                    connectionStatusTextView.text = status
                }
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "EXIT function")
    }

    private fun loginAsUserC() {
        otherUser = "userB"
        val token = clientManager.generateJWTTokenForUser("userC")
        clientManager.getClient().createSession(token) { err, sessionId ->
            when {
                err != null -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }

                else -> {
                    runOnUiThread {
                        clientManager.setSession(sessionId)
                        hideUI()
                        callContextLayout.visibility = View.VISIBLE
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = "Connected"
                        startCallButton.visibility = View.VISIBLE
                        waitingForIncomingCallTextView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun loginAsUserD() {
        otherUser = "userB"
        val token = clientManager.generateJWTTokenForUser("userD")
        clientManager.getClient().createSession(token) { err, sessionId ->
            when {
                err != null -> {
                    runOnUiThread {
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }

                else -> {
                    runOnUiThread {
                        clientManager.setSession(sessionId)
                        hideUI()
                        connectionStatusTextView.visibility = View.VISIBLE
                        connectionStatusTextView.text = "Connected"
                        startCallButton.visibility = View.GONE
                        waitingForIncomingCallTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun assignCallContext() {
        val userInput: String = callContextInputText.text.toString().trim()
        if (userInput.isNotEmpty()) {
            Toast.makeText(this, "Submitted Context: $userInput", Toast.LENGTH_SHORT).show()
            callContextVal = userInput
        } else {
            Toast.makeText(this, "Input is empty", Toast.LENGTH_SHORT).show()
            callContextVal = ""
        }
    }

    //@SuppressLint("MissingPermission")
    private fun startCall() {
        clientManager.getClient().serverCall(mapOf("to" to otherUser, "context" to callContextVal)) { err, outboundCall ->
            when {
                err != null -> {
                    runOnUiThread {
                        connectionStatusTextView.text = err.localizedMessage
                    }
                }

                else -> {
                    clientManager.setCall(outboundCall)
                    runOnUiThread {
                        hideUI()
                        endCallButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun endCall() {
        clientManager.getCall()?.let {
            clientManager.getClient().hangup(it) {
                    err ->
                when {
                    err != null -> {
                        runOnUiThread {
                            connectionStatusTextView.text = err.localizedMessage
                            callContextLayout.visibility = View.VISIBLE
                            connectionStatusTextView.visibility = View.VISIBLE
                            connectionStatusTextView.text = "Connected"
                            startCallButton.visibility = View.VISIBLE
                            endCallButton.visibility = View.GONE
                            waitingForIncomingCallTextView.visibility = View.GONE
                        }
                    }

                    else -> {
                        runOnUiThread {
                            hideUI()
                            callContextLayout.visibility = View.VISIBLE
                            connectionStatusTextView.visibility = View.VISIBLE
                            connectionStatusTextView.text = "Connected"
                            startCallButton.visibility = View.VISIBLE
                            endCallButton.visibility = View.GONE
                            waitingForIncomingCallTextView.visibility = View.GONE
                        }
                    }
                }
            }
        }
        clientManager.setCall(null)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(
                    this, permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Action to take when permission is already granted
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    // Action to take when permission was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
                }
                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        } else {
            // Device does not support required permission
            Toast.makeText(this, "No required permission", Toast.LENGTH_LONG).show()
        }
    }
}
