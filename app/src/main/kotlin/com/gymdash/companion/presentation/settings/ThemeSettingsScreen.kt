package com.gymdash.companion.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymdash.companion.data.remote.dto.ThemeColorsDto
import com.gymdash.companion.presentation.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (ThemeColorsDto) -> Unit,
    viewModel: ThemeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load themes", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            val darkThemes = uiState.defaultThemes.filter { it.type == "dark" }
            val lightThemes = uiState.defaultThemes.filter { it.type == "light" }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (darkThemes.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Dark Themes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(darkThemes) { theme ->
                        val isActive = uiState.activePreference.themeType == "default" &&
                                uiState.activePreference.themeIdentifier == theme.key
                        ThemeCard(
                            name = theme.name,
                            colors = theme.colors,
                            isActive = isActive,
                            onClick = {
                                viewModel.selectTheme("default", theme.key)
                                onThemeChanged(theme.colors)
                            }
                        )
                    }
                }

                if (lightThemes.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Light Themes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(lightThemes) { theme ->
                        val isActive = uiState.activePreference.themeType == "default" &&
                                uiState.activePreference.themeIdentifier == theme.key
                        ThemeCard(
                            name = theme.name,
                            colors = theme.colors,
                            isActive = isActive,
                            onClick = {
                                viewModel.selectTheme("default", theme.key)
                                onThemeChanged(theme.colors)
                            }
                        )
                    }
                }

                if (uiState.customThemes.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Custom Themes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(uiState.customThemes) { theme ->
                        val isActive = uiState.activePreference.themeType == "custom" &&
                                uiState.activePreference.themeIdentifier == theme.name
                        ThemeCard(
                            name = theme.name,
                            colors = theme.colors,
                            isActive = isActive,
                            onClick = {
                                viewModel.selectTheme("custom", theme.name)
                                onThemeChanged(theme.colors)
                            }
                        )
                    }
                }

                // Bottom spacing
                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    name: String,
    colors: ThemeColorsDto,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = parseHexColor(colors.bgPrimary)
    val cardColor = parseHexColor(colors.bgCard)
    val accentColor = parseHexColor(colors.accent)
    val textColor = parseHexColor(colors.textPrimary)
    val textSecondary = parseHexColor(colors.textSecondary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mini preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardColor)
                    .padding(8.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(textColor)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(textSecondary)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Theme name and color swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
                if (isActive) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Active",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Color swatches row
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    colors.accent, colors.bgCard, colors.textPrimary,
                    colors.success, colors.warning
                ).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                    )
                }
            }
        }
    }
}
