package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ScanRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateCsv(
        records: List<ScanRecord>,
        delimiter: String = ","
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility
        sb.append("\uFEFF")

        // Headers
        val headers = listOf(
            "No",
            "Waktu Scan",
            "Kode Barcode / Resi",
            "Kurir",
            "Kategori",
            "Format",
            "Status",
            "Jumlah Scan",
            "Duplikat",
            "Catatan"
        )
        sb.append(headers.joinToString(delimiter) { escapeCsv(it, delimiter) })
        sb.append("\r\n")

        // Rows
        records.forEachIndexed { index, record ->
            val formattedDate = dateFormat.format(Date(record.timestamp))
            val duplicateStr = if (record.isDuplicate) "YA (Duplikat)" else "TIDAK (Unik)"
            val row = listOf(
                (index + 1).toString(),
                formattedDate,
                record.rawCode,
                record.courierName,
                record.category,
                record.format,
                record.status,
                record.scanCount.toString(),
                duplicateStr,
                record.notes
            )
            sb.append(row.joinToString(delimiter) { escapeCsv(it, delimiter) })
            sb.append("\r\n")
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String, delimiter: String): String {
        var result = value.replace("\"", "\"\"")
        if (result.contains(delimiter) || result.contains("\"") || result.contains("\n") || result.contains("\r")) {
            result = "\"$result\""
        }
        return result
    }

    fun generateExcelXml(records: List<ScanRecord>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n")
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
        sb.append(" <Styles>\n")
        sb.append("  <Style ss:ID=\"Default\" ss:Name=\"Normal\">\n")
        sb.append("   <Alignment ss:Vertical=\"Center\"/>\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#000000\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"HeaderStyle\">\n")
        sb.append("   <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\"/>\n")
        sb.append("   <Interior ss:Color=\"#0284C7\" ss:Pattern=\"Solid\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"DuplicateStyle\">\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#DC2626\" ss:Bold=\"1\"/>\n")
        sb.append("  </Style>\n")
        sb.append("  <Style ss:ID=\"UniqueStyle\">\n")
        sb.append("   <Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Color=\"#059669\"/>\n")
        sb.append("  </Style>\n")
        sb.append(" </Styles>\n")

        sb.append(" <Worksheet ss:Name=\"Data Resi\">\n")
        sb.append("  <Table>\n")
        sb.append("   <Column ss:Width=\"40\"/>\n")
        sb.append("   <Column ss:Width=\"140\"/>\n")
        sb.append("   <Column ss:Width=\"180\"/>\n")
        sb.append("   <Column ss:Width=\"110\"/>\n")
        sb.append("   <Column ss:Width=\"100\"/>\n")
        sb.append("   <Column ss:Width=\"90\"/>\n")
        sb.append("   <Column ss:Width=\"90\"/>\n")
        sb.append("   <Column ss:Width=\"80\"/>\n")
        sb.append("   <Column ss:Width=\"90\"/>\n")
        sb.append("   <Column ss:Width=\"150\"/>\n")

        // Headers
        sb.append("   <Row ss:Height=\"24\">\n")
        val headers = listOf(
            "No", "Waktu Scan", "Kode Barcode / Resi", "Kurir",
            "Kategori", "Format", "Status", "Jumlah Scan", "Duplikat", "Catatan"
        )
        headers.forEach { h ->
            sb.append("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">$h</Data></Cell>\n")
        }
        sb.append("   </Row>\n")

        // Data Rows
        records.forEachIndexed { index, r ->
            val formattedDate = dateFormat.format(Date(r.timestamp))
            val dupText = if (r.isDuplicate) "YA (Duplikat)" else "TIDAK (Unik)"
            val dupStyle = if (r.isDuplicate) "DuplicateStyle" else "UniqueStyle"

            sb.append("   <Row ss:Height=\"20\">\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">${index + 1}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">$formattedDate</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.rawCode)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.courierName)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.category)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.format)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.status)}</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"Number\">${r.scanCount}</Data></Cell>\n")
            sb.append("    <Cell ss:StyleID=\"$dupStyle\"><Data ss:Type=\"String\">$dupText</Data></Cell>\n")
            sb.append("    <Cell><Data ss:Type=\"String\">${escapeXml(r.notes)}</Data></Cell>\n")
            sb.append("   </Row>\n")
        }

        sb.append("  </Table>\n")
        sb.append(" </Worksheet>\n")
        sb.append("</Workbook>")

        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun exportAndShare(
        context: Context,
        records: List<ScanRecord>,
        isExcel: Boolean = false,
        delimiter: String = ","
    ) {
        if (records.isEmpty()) {
            Toast.makeText(context, "Tidak ada data riwayat untuk diekspor", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = fileTimestampFormat.format(Date())
            val filename = if (isExcel) "ScanResi_$timestamp.xls" else "ScanResi_$timestamp.csv"
            val file = File(exportDir, filename)

            val content = if (isExcel) {
                generateExcelXml(records)
            } else {
                generateCsv(records, delimiter)
            }

            FileOutputStream(file).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val contentUri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = if (isExcel) "application/vnd.ms-excel" else "text/csv"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Data Barcode & Resi ($timestamp)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Bagikan / Buka File Hasil Ekspor")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengekspor data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Data Resi") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Berhasil disalin ke papan klip", Toast.LENGTH_SHORT).show()
    }
}
