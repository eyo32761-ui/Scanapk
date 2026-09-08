package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DuplicateAlertDialog
import com.example.ui.components.EditScanRecordDialog
import com.example.ui.components.ManualInputDialog
import com.example.ui.components.SamplePickerSheet
import com.example.ui.export.ExportScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.scanner.ScannerScreen
import com.example.ui.settings.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allScans by viewModel.allScans.collectAsStateWithLifecycle()
    val filteredScans by viewModel.filteredScans.collectAsStateWithLifecycle()
    val couriers by viewModel.couriers.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val duplicateCount = remember(allScans) { allScans.count { it.isDuplicate || it.scanCount > 1 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState.selectedTab) {
                            0 -> "Pemindai Barcode Resi"
                            1 -> "Riwayat Lokal (${allScans.size})"
                            2 -> "Ekspor CSV & Excel"
                            else -> "Scan Resi"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Pemindai")
                    },
                    label = { Text("Pemindai", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_scanner")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = {
                        if (duplicateCount > 0) {
                            BadgedBox(badge = { Badge { Text("$duplicateCount") } }) {
                                Icon(Icons.Default.History, contentDescription = "Riwayat")
                            }
                        } else {
                            Icon(Icons.Default.History, contentDescription = "Riwayat")
                        }
                    },
                    label = { Text("Riwayat", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_history")
                )

                NavigationBarItem(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor")
                    },
                    label = { Text("Ekspor", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_export")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                0 -> ScannerScreen(
                    uiState = uiState,
                    onBarcodeDetected = { code, format ->
                        viewModel.processScan(code, format)
                    },
                    onToggleTorch = { viewModel.toggleTorch() },
                    onOpenManualInput = { viewModel.showManualInputDialog(true) },
                    onOpenSamplePicker = { viewModel.showSamplePicker(true) },
                    onImagePicked = { uri -> viewModel.scanImageUri(uri) },
                    onDismissBanner = { viewModel.dismissBanner() }
                )
                1 -> HistoryScreen(
                    allScans = allScans,
                    filteredScans = filteredScans,
                    couriers = couriers,
                    searchQuery = uiState.searchQuery,
                    activeFilter = uiState.activeFilter,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFilterChange = { viewModel.setActiveFilter(it) },
                    onItemClick = { viewModel.showItemDetail(it) }
                )
                2 -> ExportScreen(
                    allScans = allScans
                )
            }

            // Duplicate Alert Modal
            uiState.duplicateAlert?.let { alert ->
                DuplicateAlertDialog(
                    alert = alert,
                    onDismiss = { viewModel.dismissDuplicateAlert() },
                    onRecordAnyway = { viewModel.confirmRecordDuplicate(it) }
                )
            }

            // Manual Input Dialog
            if (uiState.isManualInputDialogVisible) {
                ManualInputDialog(
                    onDismiss = { viewModel.showManualInputDialog(false) },
                    onSubmit = { code, format ->
                        viewModel.showManualInputDialog(false)
                        viewModel.processScan(code, format)
                    }
                )
            }

            // Quick Sample Picker
            if (uiState.isSamplePickerVisible) {
                SamplePickerSheet(
                    onDismiss = { viewModel.showSamplePicker(false) },
                    onSelectSample = { code, format ->
                        viewModel.processScan(code, format)
                    }
                )
            }

            // Edit Item Dialog
            uiState.selectedItemForDetail?.let { record ->
                EditScanRecordDialog(
                    record = record,
                    onDismiss = { viewModel.showItemDetail(null) },
                    onSave = { rec, status, notes ->
                        viewModel.updateRecord(rec, status, notes)
                    },
                    onDelete = { rec ->
                        viewModel.deleteRecord(rec)
                    }
                )
            }

            // Settings Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    currentMode = uiState.duplicateMode,
                    soundEnabled = uiState.soundEnabled,
                    vibrateEnabled = uiState.vibrateEnabled,
                    onModeChange = { viewModel.setDuplicateMode(it) },
                    onSoundToggle = { viewModel.setSoundEnabled(it) },
                    onVibrateToggle = { viewModel.setVibrateEnabled(it) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onDismiss = { showSettingsDialog = false }
                )
            }
        }
    }
}
