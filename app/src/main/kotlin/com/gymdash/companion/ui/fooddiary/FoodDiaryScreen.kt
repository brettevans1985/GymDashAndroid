package com.gymdash.companion.ui.fooddiary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    onNavigateToScanner: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Food Diary") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
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

            Text(
                "Tap the + button to scan a food barcode, or Search to find food by name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
