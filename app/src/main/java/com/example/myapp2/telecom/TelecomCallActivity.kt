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

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import com.example.myapp2.utils.VLogUtils


/**
 * This activity is used to launch the incoming or ongoing call. It uses special flags to be able
 * to be launched in the lockscreen and as a full-screen notification.
 */
//@RequiresApi(Build.VERSION_CODES.O)
class TelecomCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        super.onCreate(savedInstanceState)
        // The repo contains all the call logic and communication with the Telecom SDK.
        val repository =
            TelecomCallRepository.instance ?: TelecomCallRepository.create(applicationContext)

        // Set the right flags for a call type activity.
        setupCallActivity()

        setContent {
            MaterialTheme {
                Surface(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Show the in-call screen
                    TelecomCallScreen(repository) {
                        // If we receive that the called finished, finish the activity
                        finishAndRemoveTask()
                        Log.d("TelecomCallActivity", "Call finished. Finishing activity")
                    }
                }
            }
        }

        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
    }

    override fun onResume() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        super.onResume()
        // Force the service to update in case something change like Mic permissions.
        startService(
            Intent(this, TelecomCallService::class.java).apply {
                action = TelecomCallService.ACTION_UPDATE_CALL
            },
        )

        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
    }

    /**
     * Enable the calling activity to be shown in the lockscreen and dismiss the keyguard to enable
     * users to answer without unblocking.
     */
    private fun setupCallActivity() {
        val funcName: String? = object {}.javaClass.enclosingMethod?.name
        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
            )
        }

        val keyguardManager = getSystemService<KeyguardManager>()
        keyguardManager?.requestDismissKeyguard(this, null)

        VLogUtils.writeLogWithPrefix(this, funcName, "ENTER function")
    }
}