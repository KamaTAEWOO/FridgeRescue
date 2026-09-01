package com.portfolio.fridgerescue.feature.intake

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class TextExtractionResult(
    val text: String,
    val isPartial: Boolean = false,
)

fun interface IntakeTextExtractor {
    suspend fun extract(file: File, isPdf: Boolean): TextExtractionResult
}

/** Keeps receipt pixels and recognized text on-device. */
class MlKitIntakeTextExtractor : IntakeTextExtractor {
    override suspend fun extract(file: File, isPdf: Boolean): TextExtractionResult =
        withContext(Dispatchers.Default) {
            val recognizer = TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build(),
            )
            try {
                if (isPdf) extractPdf(file) { bitmap ->
                    recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
                } else {
                    val bitmap = decodeBoundedBitmap(file)
                    try {
                        TextExtractionResult(
                            text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text,
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            } finally {
                recognizer.close()
            }
        }

    private suspend fun extractPdf(
        file: File,
        recognize: suspend (Bitmap) -> String,
    ): TextExtractionResult {
        val pageTexts = mutableListOf<String>()
        var failedPage = false
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val pageLimit = minOf(renderer.pageCount, MAX_PDF_PAGES)
                repeat(pageLimit) { index ->
                    val pageText = runCatching {
                        renderer.openPage(index).use { page ->
                            val scale = minOf(
                                MAX_PDF_WIDTH.toFloat() / page.width,
                                MAX_PDF_HEIGHT.toFloat() / page.height,
                                2f,
                            )
                            val width = (page.width * scale).toInt().coerceAtLeast(1)
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                )
                                recognize(bitmap)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }.getOrElse {
                        failedPage = true
                        ""
                    }
                    if (pageText.isNotBlank()) pageTexts += pageText
                }
                return TextExtractionResult(
                    text = pageTexts.joinToString("\n"),
                    isPartial = failedPage || renderer.pageCount > pageLimit,
                )
            }
        }
    }

    private fun decodeBoundedBitmap(file: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
        require(bounds.outWidth <= MAX_SOURCE_DIMENSION && bounds.outHeight <= MAX_SOURCE_DIMENSION) {
            "Image dimensions are too large"
        }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION
        ) {
            sampleSize *= 2
        }
        val decoded = requireNotNull(
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize },
            ),
        ) { "Image could not be decoded" }
        val rotation = runCatching {
            when (ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (rotation == 0f) return decoded
        return Bitmap.createBitmap(
            decoded,
            0,
            0,
            decoded.width,
            decoded.height,
            Matrix().apply { postRotate(rotation) },
            true,
        ).also { rotated ->
            if (rotated !== decoded) decoded.recycle()
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { continuation.resume(it) }
            addOnFailureListener { continuation.resumeWithException(it) }
            addOnCanceledListener { continuation.cancel() }
        }

    private companion object {
        const val MAX_SOURCE_DIMENSION = 20_000
        const val MAX_IMAGE_DIMENSION = 2_048
        const val MAX_PDF_WIDTH = 1_600
        const val MAX_PDF_HEIGHT = 2_400
        const val MAX_PDF_PAGES = 5
    }
}
