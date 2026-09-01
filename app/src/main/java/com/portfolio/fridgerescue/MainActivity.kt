package com.portfolio.fridgerescue

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.portfolio.fridgerescue.feature.intake.SharedContentReceiver
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueRoute
import com.portfolio.fridgerescue.ui.theme.FridgeRescueTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FridgeRescueTheme {
                RescueRoute()
            }
        }
        receiveSharedContent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveSharedContent(intent)
    }

    private fun receiveSharedContent(intent: Intent) {
        val repository = (application as FridgeRescueApplication).container.intakeDraftRepository
        lifecycleScope.launch {
            SharedContentReceiver(applicationContext, repository).receive(intent)
        }
    }
}
