package com.example.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanRecord
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportScreen(
    allScans: List<ScanRecord>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isExcelFormat by remember { mutableStateOf(true) }
    var selectedDelimiter by remember { mutableStateOf(",") }
    var selectedScope by remember { mutableStateOf("Semua Data") }

    val scopeOptions = listOf("Semua Data", "Hanya Resi Logistik", "Hanya Duplikat", "Hari Ini")

    val dataToExport = remember(allScans, selectedScope) {
        when (selectedScope) {
            "Semua Data" -> allScans
            "Hanya Resi Logistik" -> allScans.filter { it.category == "Resi Logistik" }
            "Hanya Duplikat" -> allScans.filter { it.isDuplicate || it.scanCount > 1 }
            "Hari Ini" -> {
                val now = System.currentTimeMillis()
                allScans.filter { (now - it.timestamp) < 24 * 60 * 60 * 1000L }
            }
            else -> allScans
        }
    }

    val courierDistribution = remember(dataToExport) {
        dataToExport.groupBy { it.courierName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ekspor CSV / Excel",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dataToExport.size} baris data siap diekspor",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Format Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Pilih Format File:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isExcelFormat,
                        onClick = { isExcelFormat = true },
                        label = { Text("Microsoft Excel (.xls)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).testTag("format_excel_chip")
                    )
                    FilterChip(
                        selected = !isExcelFormat,
                        onClick = { isExcelFormat = false },
                        label = { Text("Standard CSV (.csv)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f).testTag("format_csv_chip")
                    )
                }

                // If CSV, allow delimiter selection
                if (!isExcelFormat) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pemisah Delimiter:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedDelimiter == ",",
                            onClick = { selectedDelimiter = "," },
                            label = { Text("Koma (,)") }
                        )
                        FilterChip(
                            selected = selectedDelimiter == ";",
                            onClick = { selectedDelimiter = ";" },
                            label = { Text("Titik Koma (;) - Excel ID") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. Filter Data Ekspor:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scopeOptions.forEach { scope ->
                        FilterChip(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                            label = { Text(scope, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Button(
            onClick = {
                ExportHelper.exportAndShare(
                    context = context,
                    records = dataToExport,
                    isExcel = isExcelFormat,
                    delimiter = selectedDelimiter
                )
            },
            enabled = dataToExport.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("export_share_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ekspor & Bagikan File (${if (isExcelFormat) "Excel" else "CSV"})")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val csvContent = ExportHelper.generateCsv(dataToExport, selectedDelimiter)
                ExportHelper.copyToClipboard(context, csvContent, "Data Ekspor Resi")
            },
            enabled = dataToExport.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("copy_csv_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salin Teks CSV ke Clipboard")
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Breakdown distribution
        if (courierDistribution.isNotEmpty()) {
            Text(
                text = "Distribusi Ekspor Berdasarkan Kurir:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                courierDistribution.forEach { (courier, count) ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = courier,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$count resi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Preview Table
        Text(
            text = "Pratinjau Tabel (10 Data Pertama):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (dataToExport.isEmpty()) {
            Text(
                text = "Tidak ada data untuk dipratinjau.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val previewItems = dataToExport.take(10)
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    ) {
                        Text("No", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                        Text("Waktu", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        Text("Kode Resi / Barcode", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(160.dp))
                        Text("Kurir", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(100.dp))
                        Text("Status", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        Text("Duplikat", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                    }

                    // Rows
                    previewItems.forEachIndexed { index, r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", fontSize = 11.sp, modifier = Modifier.width(36.dp))
                            Text(dateFormat.format(Date(r.timestamp)), fontSize = 11.sp, modifier = Modifier.width(70.dp))
                            Text(r.rawCode, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(160.dp))
                            Text(r.courierName, fontSize = 11.sp, modifier = Modifier.width(100.dp))
                            Text(r.status, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                            Text(
                                if (r.isDuplicate) "Ya" else "-",
                                color = if (r.isDuplicate) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (r.isDuplicate) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier.width(70.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
