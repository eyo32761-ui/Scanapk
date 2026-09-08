package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ScanRecord
import com.example.data.repository.ScanRepository
import com.example.util.CourierDetector
import com.example.util.SoundAndHaptic
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DuplicateMode(val title: String, val description: String) {
    ALERT_CONFIRM("Peringatan & Konfirmasi", "Tampilkan dialog konfirmasi saat mendeteksi duplikat"),
    AUTO_REJECT("Tolak Otomatis", "Blokir dan bunyikan peringatan tanpa menyimpan duplikat"),
    AUTO_RECORD("Catat Otomatis", "Langsung simpan dengan penanda duplikat")
}

data class DuplicateAlert(
    val existingRecord: ScanRecord,
    val incomingCode: String,
    val incomingFormat: String,
    val occurrenceCount: Int
)

data class ScanUiState(
    val selectedTab: Int = 0, // 0: Pemindai, 1: Riwayat, 2: Ekspor, 3: Pengaturan
    val searchQuery: String = "",
    val activeFilter: String = "Semua", // "Semua", "Resi Logistik", "Duplikat", "QR Code", "Hari Ini", atau nama kurir
    val isTorchOn: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val duplicateMode: DuplicateMode = DuplicateMode.ALERT_CONFIRM,
    val duplicateAlert: DuplicateAlert? = null,
    val bannerMessage: String? = null,
    val isBannerError: Boolean = false,
    val selectedItemForDetail: ScanRecord? = null,
    val isManualInputDialogVisible: Boolean = false,
    val isSamplePickerVisible: Boolean = false,
    val isExportPreviewVisible: Boolean = false
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    private val soundAndHaptic: SoundAndHaptic = SoundAndHaptic(application)

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ScanRepository(db.scanDao())
    }

    val allScans: StateFlow<List<ScanRecord>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val couriers: StateFlow<List<String>> = repository.allCouriers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Scans reactive stream
    val filteredScans: StateFlow<List<ScanRecord>> = combine(
        allScans,
        _uiState
    ) { scans, state ->
        val query = state.searchQuery.trim().lowercase()
        val filter = state.activeFilter

        scans.filter { record ->
            val matchesQuery = query.isEmpty() ||
                    record.rawCode.lowercase().contains(query) ||
                    record.courierName.lowercase().contains(query) ||
                    record.notes.lowercase().contains(query) ||
                    record.status.lowercase().contains(query)

            val matchesFilter = when (filter) {
                "Semua" -> true
                "Resi Logistik" -> record.category == "Resi Logistik"
                "Duplikat" -> record.isDuplicate || record.scanCount > 1
                "QR Code" -> record.category == "QR Code" || record.format == "QR_CODE"
                "Hari Ini" -> {
                    val now = System.currentTimeMillis()
                    val diff = now - record.timestamp
                    diff < 24 * 60 * 60 * 1000L
                }
                else -> record.courierName.equals(filter, ignoreCase = true)
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setActiveFilter(filter: String) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(soundEnabled = enabled)
    }

    fun setVibrateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrateEnabled = enabled)
    }

    fun setDuplicateMode(mode: DuplicateMode) {
        _uiState.value = _uiState.value.copy(duplicateMode = mode)
    }

    fun showManualInputDialog(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isManualInputDialogVisible = visible)
    }

    fun showSamplePicker(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isSamplePickerVisible = visible)
    }

    fun showItemDetail(record: ScanRecord?) {
        _uiState.value = _uiState.value.copy(selectedItemForDetail = record)
    }

    fun dismissBanner() {
        _uiState.value = _uiState.value.copy(bannerMessage = null)
    }

    // MAIN SCANNING LOGIC WITH DUPLICATE DETECTION
    fun processScan(rawCode: String, format: String = "CODE_128") {
        val cleanCode = rawCode.trim()
        if (cleanCode.isEmpty()) return

        // Prevent processing if duplicate alert dialog is currently active
        if (_uiState.value.duplicateAlert != null) return

        viewModelScope.launch {
            val existing = repository.findFirstByRawCode(cleanCode)

            if (existing != null) {
                // DUPLICATE DETECTED
                val occurrenceCount = repository.countOccurrences(cleanCode)

                if (_uiState.value.soundEnabled) {
                    soundAndHaptic.playDuplicateWarning()
                }
                if (_uiState.value.vibrateEnabled) {
                    soundAndHaptic.vibrateDuplicate()
                }

                when (_uiState.value.duplicateMode) {
                    DuplicateMode.AUTO_REJECT -> {
                        _uiState.value = _uiState.value.copy(
                            bannerMessage = "DUPLIKAT DITOLAK: $cleanCode (${existing.courierName}) sudah pernah discan!",
                            isBannerError = true
                        )
                    }
                    DuplicateMode.AUTO_RECORD -> {
                        // Record immediately with duplicate flag
                        val detection = CourierDetector.detect(cleanCode, format)
                        val newDuplicateRecord = ScanRecord(
                            rawCode = cleanCode,
                            format = format,
                            courierName = detection.courierName,
                            category = detection.category,
                            timestamp = System.currentTimeMillis(),
                            scanCount = occurrenceCount + 1,
                            isDuplicate = true,
                            status = existing.status,
                            notes = "Duplikat ke-${occurrenceCount + 1}"
                        )
                        repository.insertScan(newDuplicateRecord)
                        _uiState.value = _uiState.value.copy(
                            bannerMessage = "Duplikat Dicatat: $cleanCode (Scan ke-${occurrenceCount + 1})",
                            isBannerError = false
                        )
                    }
                    DuplicateMode.ALERT_CONFIRM -> {
                        // Show confirmation dialog with full details
                        _uiState.value = _uiState.value.copy(
                            duplicateAlert = DuplicateAlert(
                                existingRecord = existing,
                                incomingCode = cleanCode,
                                incomingFormat = format,
                                occurrenceCount = occurrenceCount
                            )
                        )
                    }
                }
            } else {
                // NEW UNIQUE CODE
                val detection = CourierDetector.detect(cleanCode, format)
                val newRecord = ScanRecord(
                    rawCode = cleanCode,
                    format = format,
                    courierName = detection.courierName,
                    category = detection.category,
                    timestamp = System.currentTimeMillis(),
                    scanCount = 1,
                    isDuplicate = false,
                    status = "Diterima",
                    notes = ""
                )
                repository.insertScan(newRecord)

                if (_uiState.value.soundEnabled) {
                    soundAndHaptic.playBeep()
                }
                if (_uiState.value.vibrateEnabled) {
                    soundAndHaptic.vibrateSuccess()
                }

                _uiState.value = _uiState.value.copy(
                    bannerMessage = "Berhasil: [${detection.courierName}] $cleanCode",
                    isBannerError = false
                )
            }
        }
    }

    // User chooses to skip duplicate
    fun dismissDuplicateAlert() {
        _uiState.value = _uiState.value.copy(
            duplicateAlert = null,
            bannerMessage = "Duplikat dilewati",
            isBannerError = false
        )
    }

    // User chooses to record the duplicate anyway
    fun confirmRecordDuplicate(alert: DuplicateAlert) {
        viewModelScope.launch {
            val detection = CourierDetector.detect(alert.incomingCode, alert.incomingFormat)
            val newDuplicateRecord = ScanRecord(
                rawCode = alert.incomingCode,
                format = alert.incomingFormat,
                courierName = detection.courierName,
                category = detection.category,
                timestamp = System.currentTimeMillis(),
                scanCount = alert.occurrenceCount + 1,
                isDuplicate = true,
                status = alert.existingRecord.status,
                notes = "Duplikat ke-${alert.occurrenceCount + 1}"
            )
            repository.insertScan(newDuplicateRecord)

            _uiState.value = _uiState.value.copy(
                duplicateAlert = null,
                bannerMessage = "Duplikat tetap dicatat (Scan ke-${alert.occurrenceCount + 1})",
                isBannerError = false
            )
        }
    }

    // Update item status or notes
    fun updateRecord(record: ScanRecord, newStatus: String, newNotes: String) {
        viewModelScope.launch {
            repository.updateScan(record.copy(status = newStatus, notes = newNotes))
            _uiState.value = _uiState.value.copy(selectedItemForDetail = null)
        }
    }

    // Delete single record
    fun deleteRecord(record: ScanRecord) {
        viewModelScope.launch {
            repository.deleteScan(record)
            if (_uiState.value.selectedItemForDetail?.id == record.id) {
                _uiState.value = _uiState.value.copy(selectedItemForDetail = null)
            }
        }
    }

    // Clear entire scan history
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAll()
            _uiState.value = _uiState.value.copy(
                bannerMessage = "Semua riwayat pemindaian telah dibersihkan",
                isBannerError = false
            )
        }
    }

    // Scan image picked from gallery
    fun scanImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(getApplication(), uri)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                        if (barcode != null && barcode.rawValue != null) {
                            val formatName = when (barcode.format) {
                                Barcode.FORMAT_QR_CODE -> "QR_CODE"
                                Barcode.FORMAT_CODE_128 -> "CODE_128"
                                Barcode.FORMAT_EAN_13 -> "EAN_13"
                                else -> "BARCODE"
                            }
                            processScan(barcode.rawValue!!, formatName)
                        } else {
                            _uiState.value = _uiState.value.copy(
                                bannerMessage = "Tidak ditemukan barcode / QR pada foto tersebut",
                                isBannerError = true
                            )
                        }
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            bannerMessage = "Gagal memproses gambar: ${e.localizedMessage}",
                            isBannerError = true
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    bannerMessage = "Gagal membuka gambar: ${e.localizedMessage}",
                    isBannerError = true
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundAndHaptic.release()
    }
}
