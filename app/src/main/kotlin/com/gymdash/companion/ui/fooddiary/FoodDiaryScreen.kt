package com.gymdash.companion.ui.fooddiary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDiaryScreen(
    repository: FoodDiaryRepository,
    waterTrackerViewModel: WaterTrackerViewModel,
    onNavigateToScanner: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBuilder: () -> Unit = {}
) {
    val waterState by waterTrackerViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Food Diary") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onNavigateToBuilder,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text("Build", modifier = Modifier.padding(horizontal = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = onNavigateToSearch,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("Search", modifier = Modifier.padding(horizontal = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(onClick = onNavigateToScanner) {
                    Icon(Icons.Default.Add, contentDescription = "Scan Food")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Today's Food Log",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Water tracker card
            WaterTrackerCard(
                currentMl = waterState.currentMl,
                goalMl = waterState.goalMl,
                glasses = waterState.glasses,
                progress = waterState.progress,
                onAddGlass = { waterTrackerViewModel.addGlass() },
                onRemoveGlass = { waterTrackerViewModel.removeGlass() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Tap the + button to scan a food barcode, or Search to find food by name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WaterTrackerCard(
    currentMl: Int,
    goalMl: Int,
    glasses: Int,
    progress: Float,
    onAddGlass: () -> Unit,
    onRemoveGlass: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Water Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "$currentMl / $goalMl ml",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onRemoveGlass,
                    enabled = currentMl > 0
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Remove glass")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    "$glasses glasses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(16.dp))

                FilledTonalIconButton(onClick = onAddGlass) {
                    Icon(Icons.Default.Add, contentDescription = "Add glass")
                }
            }
        }
    }
}
