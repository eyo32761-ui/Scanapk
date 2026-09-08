package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawCode: String,
    val format: String, // e.g., "CODE_128", "QR_CODE", "EAN_13"
    val courierName: String, // "JNE", "J&T Express", "SiCepat", "Shopee Xpress", "Umum", etc.
    val category: String, // "Resi Logistik", "QR Code", "Barcode Produk", "Teks"
    val timestamp: Long = System.currentTimeMillis(),
    val scanCount: Int = 1,
    val isDuplicate: Boolean = false,
    val status: String = "Diterima", // "Diterima", "Disortir", "Dikirim", "Pending", "Bermasalah"
    val notes: String = ""
)
