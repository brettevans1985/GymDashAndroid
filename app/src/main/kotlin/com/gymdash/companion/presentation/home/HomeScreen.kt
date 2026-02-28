package com.gymdash.companion.presentation.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymdash.companion.R
import com.gymdash.companion.domain.model.HealthMetric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.onPermissionsResult()
    }

    LaunchedEffect(uiState.needsPermissionRequest) {
        if (uiState.needsPermissionRequest) {
            permissionLauncher.launch(HomeViewModel.HEALTH_PERMISSIONS)
        }
    }

    // Sync Preview Bottom Sheet
    if (uiState.syncPreview !is SyncPreviewState.Hidden) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSyncPreview,
            sheetState = sheetState
        ) {
            when (val preview = uiState.syncPreview) {
                is SyncPreviewState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.sync_preview_reading),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                is SyncPreviewState.Ready -> {
                    val allNonEmpty = preview.metricCounts.filter { it.value > 0 }.keys
                    val allSelected = allNonEmpty.isNotEmpty() && allNonEmpty == preview.selectedMetrics
                    val hasData = allNonEmpty.isNotEmpty()

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.sync_preview_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (hasData) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.sync_preview_select_all),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = allSelected,
                                        onCheckedChange = { viewModel.toggleAllMetrics() }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!hasData) {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.sync_preview_no_data),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::dismissSyncPreview,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.sync_preview_cancel))
                            }
                        } else {
                            // Metric list
                            LazyColumn(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                items(HealthMetric.entries) { metric ->
                                    val count = preview.metricCounts[metric] ?: 0
                                    val isSelected = metric in preview.selectedMetrics
                                    val enabled = count > 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = metric.displayName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (enabled)
                                                    MaterialTheme.colorScheme.onSurface
                                                else
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                            Text(
                                                text = if (count == 1)
                                                    stringResource(R.string.sync_preview_record, count)
                                                else
                                                    stringResource(R.string.sync_preview_records, count),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (enabled)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                            )
                                        }
                                        Switch(
                                            checked = isSelected,
                                            onCheckedChange = { viewModel.toggleMetric(metric) },
                                            enabled = enabled
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Footer buttons
                            Button(
                                onClick = viewModel::confirmSync,
                                enabled = preview.selectedMetrics.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.sync_preview_send, preview.selectedCount))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = viewModel::dismissSyncPreview,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.sync_preview_cancel))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                is SyncPreviewState.Sending -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.sync_preview_sending),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                is SyncPreviewState.Hidden -> { /* handled by outer if */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isHealthConnectAvailable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Health Connect Not Available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Health Connect is not installed on this device. Please install it from the Google Play Store to sync health data.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else if (!uiState.hasHealthPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        permissionLauncher.launch(HomeViewModel.HEALTH_PERMISSIONS)
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Health Connect permissions required",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(HomeViewModel.HEALTH_PERMISSIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            // Live Heart Rate Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isLiveHeartRateActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (uiState.isLiveHeartRateActive)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live Heart Rate",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (uiState.heartRateStatus != null) {
                            Text(
                                text = uiState.heartRateStatus!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.currentHeartRate != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${uiState.currentHeartRate}",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BPM",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = viewModel::toggleLiveHeartRate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isLiveHeartRateActive) "Stop Live HR" else "Start Live HR"
                        )
                    }
                }
            }

            // Sync Status Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (uiState.lastSyncTime != null) {
                            stringResource(R.string.home_last_sync, uiState.lastSyncTime!!)
                        } else {
                            "No sync yet"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.lastSyncResult != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.lastSyncResult!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val isPreviewActive = uiState.syncPreview !is SyncPreviewState.Hidden
            Button(
                onClick = viewModel::syncNow,
                enabled = !uiState.isSyncing && !isPreviewActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.home_sync_button))
                }
            }
        }
    }
}
