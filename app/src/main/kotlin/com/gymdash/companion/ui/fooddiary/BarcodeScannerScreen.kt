package com.gymdash.companion.ui.fooddiary

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.gymdash.companion.data.remote.dto.CreateFoodDiaryEntryRequest
import com.gymdash.companion.data.remote.dto.FoodLookupResponse
import com.gymdash.companion.domain.repository.FoodDiaryRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    repository: FoodDiaryRepository,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showManualInput by remember { mutableStateOf(false) }
    var scannedBarcode by remember { mutableStateOf("") }
    var product by remember { mutableStateOf<FoodLookupResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf("1.0") }
    var selectedMeal by remember { mutableIntStateOf(0) }
    val mealNames = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    // Track if we've already processed a barcode to avoid duplicate scans
    var barcodeProcessed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) showManualInput = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun lookupBarcode(barcode: String) {
        if (isLoading) return
        scannedBarcode = barcode
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = repository.lookupBarcode(barcode)
                if (result != null) {
                    product = result
                } else {
                    errorMessage = "Product not found for barcode $barcode"
                }
            } catch (e: Exception) {
                errorMessage = "Lookup failed: ${e.message}"
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Food") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (product == null) {
                if (!showManualInput && hasCameraPermission && !isLoading) {
                    // Camera preview with barcode scanning
                    Text(
                        "Point camera at a barcode",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()

                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val barcodeScanner = BarcodeScanning.getClient()
                                    val analysisExecutor = Executors.newSingleThreadExecutor()

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { analysis ->
                                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                                @androidx.camera.core.ExperimentalGetImage
                                                val mediaImage = imageProxy.image
                                                if (mediaImage != null && !barcodeProcessed) {
                                                    val inputImage = InputImage.fromMediaImage(
                                                        mediaImage,
                                                        imageProxy.imageInfo.rotationDegrees
                                                    )
                                                    barcodeScanner.process(inputImage)
                                                        .addOnSuccessListener { barcodes ->
                                                            for (barcode in barcodes) {
                                                                val rawValue = barcode.rawValue ?: continue
                                                                val format = barcode.format
                                                                if (format == Barcode.FORMAT_EAN_13 ||
                                                                    format == Barcode.FORMAT_EAN_8 ||
                                                                    format == Barcode.FORMAT_UPC_A ||
                                                                    format == Barcode.FORMAT_UPC_E
                                                                ) {
                                                                    barcodeProcessed = true
                                                                    lookupBarcode(rawValue)
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("BarcodeScanner", "Scan failed", e)
                                                        }
                                                        .addOnCompleteListener {
                                                            imageProxy.close()
                                                        }
                                                } else {
                                                    imageProxy.close()
                                                }
                                            }
                                        }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        Log.e("BarcodeScanner", "Camera bind failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showManualInput = true },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text("Enter barcode manually")
                    }
                } else {
                    // Manual barcode input or loading state
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!hasCameraPermission) {
                            Text(
                                "Camera permission denied. Enter barcode manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Text("Grant camera permission")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        } else if (isLoading) {
                            Text(
                                "Looking up barcode $scannedBarcode...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Text(
                                "Enter barcode manually",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (!isLoading) {
                            OutlinedTextField(
                                value = scannedBarcode,
                                onValueChange = { scannedBarcode = it },
                                label = { Text("Barcode") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { lookupBarcode(scannedBarcode) },
                                enabled = scannedBarcode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Look Up")
                            }

                            if (hasCameraPermission) {
                                TextButton(onClick = {
                                    showManualInput = false
                                    barcodeProcessed = false
                                    errorMessage = null
                                }) {
                                    Text("Back to camera")
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    barcodeProcessed = false
                                    errorMessage = null
                                    if (hasCameraPermission) showManualInput = false
                                }) {
                                    Text("Scan again")
                                }
                                TextButton(onClick = onNavigateToSearch) {
                                    Text("Search manually")
                                }
                            }
                        }
                    }
                }
            } else {
                // Product found - show details and add form
                Column(modifier = Modifier.padding(16.dp)) {
                    val p = product!!
                    Text(p.name, style = MaterialTheme.typography.headlineSmall)
                    if (p.brand != null) {
                        Text(p.brand, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Per serving${p.servingUnit?.let { " ($it)" } ?: ""}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Calories: ${p.caloriesPerServing ?: "—"} kcal")
                            Text("Protein: ${p.proteinPerServing ?: "—"}g")
                            Text("Carbs: ${p.carbsPerServing ?: "—"}g")
                            Text("Fat: ${p.fatPerServing ?: "—"}g")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity (servings)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Meal", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mealNames.forEachIndexed { index, name ->
                            FilterChip(
                                selected = selectedMeal == index,
                                onClick = { selectedMeal = index },
                                label = { Text(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val qty = quantity.toDoubleOrNull() ?: 1.0
                            isLoading = true
                            scope.launch {
                                try {
                                    repository.createEntry(
                                        CreateFoodDiaryEntryRequest(
                                            foodProductId = p.id,
                                            calendarDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                                            mealCategory = selectedMeal,
                                            quantity = qty,
                                            productName = p.name,
                                            barcode = p.barcode,
                                            servingSize = p.servingSize ?: 100.0,
                                            servingUnit = p.servingUnit,
                                            caloriesPerServing = p.caloriesPerServing ?: 0.0,
                                            proteinPerServing = p.proteinPerServing ?: 0.0,
                                            carbsPerServing = p.carbsPerServing ?: 0.0,
                                            fatPerServing = p.fatPerServing ?: 0.0,
                                            fibrePerServing = p.fibrePerServing ?: 0.0,
                                            saltPerServing = p.saltPerServing ?: 0.0,
                                            entrySource = 0  // BarcodeScan
                                        )
                                    )
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to add entry: ${e.message}"
                                }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "Adding..." else "Add to Diary")
                    }
                }
            }
        }
    }
}
