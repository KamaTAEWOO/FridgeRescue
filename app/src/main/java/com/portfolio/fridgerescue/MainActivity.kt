package com.portfolio.fridgerescue

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import com.portfolio.fridgerescue.feature.intake.SharedContentReceiver
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueRoute
import com.portfolio.fridgerescue.ui.theme.FridgeRescueTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var pendingCaptureFile: File? = null
    private val receiptCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val file = pendingCaptureFile.also { pendingCaptureFile = null }
            ?: return@registerForActivityResult
        if (!saved) {
            file.delete()
            return@registerForActivityResult
        }
        receiveSharedContent(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, receiptUri(file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }
    private val receiptPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@registerForActivityResult
        receiveSharedContent(
            Intent(Intent.ACTION_SEND).apply {
                type = contentResolver.getType(uri) ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FridgeRescueTheme {
                RescueRoute(
                    onCaptureReceipt = ::captureReceipt,
                    onPickReceipt = ::pickReceipt,
                )
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

    private fun pickReceipt() {
        receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun captureReceipt() {
        val directory = File(cacheDir, "receipt-capture").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        pendingCaptureFile = file
        receiptCamera.launch(receiptUri(file))
    }

    private fun receiptUri(file: File) = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        file,
    )
}
