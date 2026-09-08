package com.example.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(
    private val onBarcodeDetected: (code: String, format: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)
    private var lastAnalyzedTimestamp = 0L
    private val throttleMillis = 1200L
    private var isLocked = false

    fun unlock() {
        isLocked = false
    }

    fun lock() {
        isLocked = true
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (isLocked || (currentTimestamp - lastAnalyzedTimestamp < throttleMillis)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty() && !isLocked) {
                        val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        firstBarcode?.rawValue?.let { rawValue ->
                            lastAnalyzedTimestamp = currentTimestamp
                            val formatName = getFormatName(firstBarcode.format)
                            onBarcodeDetected(rawValue, formatName)
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignore transient analysis failures
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        try {
            scanner.close()
        } catch (_: Exception) {}
    }

    private fun getFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_PDF417 -> "PDF_417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "BARCODE"
        }
    }
}
