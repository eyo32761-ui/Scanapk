package com.example.util

data class CourierDetectionResult(
    val courierName: String,
    val category: String, // "Resi Logistik", "QR Code", "Barcode Produk", "Teks / Lainnya"
    val description: String,
    val badgeColorHex: Long
)

object CourierDetector {

    fun detect(rawInput: String, format: String = ""): CourierDetectionResult {
        val trimmed = rawInput.trim()
        val upper = trimmed.uppercase()

        // 1. Check if URL
        if (upper.startsWith("HTTP://") || upper.startsWith("HTTPS://") || upper.startsWith("WWW.")) {
            return CourierDetectionResult(
                courierName = "Web Link",
                category = "QR Code",
                description = "URL / Tautan Web",
                badgeColorHex = 0xFF0284C7
            )
        }

        // 2. Shopee Xpress / SPX
        if (upper.startsWith("SPXID") || upper.startsWith("SPX") || (upper.startsWith("ID") && upper.length in 12..16 && upper.any { it.isDigit() })) {
            return CourierDetectionResult(
                courierName = "Shopee Xpress",
                category = "Resi Logistik",
                description = "Resi SPX Express",
                badgeColorHex = 0xFFEE4D2D
            )
        }

        // 3. J&T Express
        if (upper.matches(Regex("^(JP|JX|JZ|JS|JD|JT)[0-9A-Z]{8,14}$"))) {
            return CourierDetectionResult(
                courierName = "J&T Express",
                category = "Resi Logistik",
                description = "Resi J&T Express",
                badgeColorHex = 0xFFDC2626
            )
        }

        // 4. SiCepat
        if ((trimmed.length == 12 && trimmed.startsWith("00") && trimmed.all { it.isDigit() }) || upper.startsWith("TKP") || upper.startsWith("SIC")) {
            return CourierDetectionResult(
                courierName = "SiCepat",
                category = "Resi Logistik",
                description = "Resi SiCepat Ekspres",
                badgeColorHex = 0xFFBE123C
            )
        }

        // 5. Anteraja
        if ((trimmed.length == 14 && trimmed.startsWith("100") && trimmed.all { it.isDigit() }) || upper.startsWith("ANTR")) {
            return CourierDetectionResult(
                courierName = "Anteraja",
                category = "Resi Logistik",
                description = "Resi Anteraja",
                badgeColorHex = 0xFF7C3AED
            )
        }

        // 6. Ninja Xpress
        if (upper.startsWith("NLID") || upper.startsWith("NVID") || upper.startsWith("NINJA") || upper.startsWith("SHP")) {
            return CourierDetectionResult(
                courierName = "Ninja Xpress",
                category = "Resi Logistik",
                description = "Resi Ninja Xpress",
                badgeColorHex = 0xFFC026D3
            )
        }

        // 7. JNE
        if (upper.startsWith("JNE") ||
            (trimmed.length in 15..16 && trimmed.all { it.isDigit() }) ||
            upper.matches(Regex("^(CGK|BDO|SUB|SOC|JOG|MES|UPG|PKU)[0-9A-Z]{10,14}$"))
        ) {
            return CourierDetectionResult(
                courierName = "JNE Express",
                category = "Resi Logistik",
                description = "Resi JNE Logistics",
                badgeColorHex = 0xFF0284C7
            )
        }

        // 8. POS Indonesia
        if (upper.startsWith("POS") || upper.matches(Regex("^P[0-9]{11,15}$")) || (trimmed.length in 11..13 && trimmed.all { it.isDigit() } && trimmed.startsWith("11"))) {
            return CourierDetectionResult(
                courierName = "POS Indonesia",
                category = "Resi Logistik",
                description = "Resi POS Indonesia / Pos Laju",
                badgeColorHex = 0xFFEA580C
            )
        }

        // 9. TIKI
        if (upper.startsWith("TIKI") || (trimmed.length == 12 && (trimmed.startsWith("66") || trimmed.startsWith("03") || trimmed.startsWith("99")) && trimmed.all { it.isDigit() })) {
            return CourierDetectionResult(
                courierName = "TIKI",
                category = "Resi Logistik",
                description = "Resi TIKI Logistics",
                badgeColorHex = 0xFF059669
            )
        }

        // 10. Lion Parcel
        if (upper.startsWith("LP") || upper.startsWith("11-") || upper.startsWith("99-") || upper.startsWith("LION")) {
            return CourierDetectionResult(
                courierName = "Lion Parcel",
                category = "Resi Logistik",
                description = "Resi Lion Parcel",
                badgeColorHex = 0xFFE11D48
            )
        }

        // 11. ID Express
        if (upper.startsWith("IDE") || upper.startsWith("IDS") || upper.startsWith("IDP")) {
            return CourierDetectionResult(
                courierName = "ID Express",
                category = "Resi Logistik",
                description = "Resi ID Express",
                badgeColorHex = 0xFFD97706
            )
        }

        // 12. Wahana
        if (upper.startsWith("WAHANA") || upper.matches(Regex("^[A-Z]{2}[0-9]{8}$"))) {
            return CourierDetectionResult(
                courierName = "Wahana",
                category = "Resi Logistik",
                description = "Resi Wahana Express",
                badgeColorHex = 0xFF0D9488
            )
        }

        // 13. General barcode products (EAN / UPC)
        if (format.contains("EAN", ignoreCase = true) || format.contains("UPC", ignoreCase = true) ||
            (trimmed.length in listOf(8, 12, 13) && trimmed.all { it.isDigit() })
        ) {
            return CourierDetectionResult(
                courierName = "Barcode Produk",
                category = "Barcode Produk",
                description = "Kode Produk Retail (EAN/UPC)",
                badgeColorHex = 0xFF475569
            )
        }

        // 14. QR Code general
        if (format.contains("QR", ignoreCase = true)) {
            return CourierDetectionResult(
                courierName = "QR Code",
                category = "QR Code",
                description = "Data QR Matrix",
                badgeColorHex = 0xFF4F46E5
            )
        }

        // 15. Default general code
        return CourierDetectionResult(
            courierName = "Umum",
            category = "Resi / Kode",
            description = "Kode Batang Terdeteksi",
            badgeColorHex = 0xFF64748B
        )
    }

    // Sample data for quick testing in emulator / demo mode
    data class SampleCourier(
        val courier: String,
        val sampleCode: String,
        val format: String = "CODE_128"
    )

    val sampleReceipts = listOf(
        SampleCourier("JNE Express", "0112345678901234", "CODE_128"),
        SampleCourier("J&T Express", "JP9876543210", "CODE_128"),
        SampleCourier("SiCepat", "002987654321", "CODE_128"),
        SampleCourier("Shopee Xpress", "SPXID04891238910B", "CODE_128"),
        SampleCourier("Anteraja", "10002345678901", "CODE_128"),
        SampleCourier("Ninja Xpress", "NLID20240901234A", "CODE_128"),
        SampleCourier("POS Indonesia", "P2409123456789", "CODE_128"),
        SampleCourier("QR Resi Toko", "https://cekresi.com/?no=JP9876543210", "QR_CODE")
    )
}
