package com.portfolio.fridgerescue.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.portfolio.fridgerescue.R
import com.portfolio.fridgerescue.feature.intake.domain.ReceiveBarcodeUseCase
import com.portfolio.fridgerescue.feature.intake.data.SharedContentReceiver
import com.portfolio.fridgerescue.feature.rescue.presentation.RescueRoute
import com.portfolio.fridgerescue.core.designsystem.theme.FridgeRescueTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 카메라·Photo Picker·바코드 스캐너처럼 Activity가 소유해야 하는 시스템 UI만 연결한다.
 * 데이터 처리와 화면 상태는 주입된 입력 처리기와 `RescueViewModel`에 위임한다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sharedContentReceiver: SharedContentReceiver
    @Inject lateinit var receiveBarcode: ReceiveBarcodeUseCase

    // TakePicture 결과에는 URI가 다시 전달되지 않으므로 요청 시 만든 파일을 잠시 보관한다.
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
                    onScanBarcode = ::scanBarcode,
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
        lifecycleScope.launch {
            sharedContentReceiver.receive(intent)
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

    private fun scanBarcode() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODABAR,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(this, options).startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue.isNullOrBlank()) {
                    Toast.makeText(this, R.string.barcode_empty_error, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                lifecycleScope.launch {
                    receiveBarcode(rawValue, barcode.format.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.barcode_scan_error, Toast.LENGTH_SHORT).show()
            }
    }

    private fun receiptUri(file: File) = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        file,
    )
}
