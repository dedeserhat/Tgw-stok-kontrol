package com.tgw.stock.ui.barcode

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.tgw.stock.ui.common.ProductPickerField
import com.tgw.stock.ui.common.tgwViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    onBack: () -> Unit,
    onProductFound: (Long) -> Unit,
    onCreateNewProduct: (String) -> Unit
) {
    val viewModel: BarcodeScanViewModel = tgwViewModel { BarcodeScanViewModel(it.productRepository) }
    val result by viewModel.result.collectAsStateWithLifecycle()
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) cameraPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(result) {
        val r = result
        if (r is BarcodeResult.Found) onProductFound(r.productId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(onBarcodeDetected = viewModel::onBarcodeDetected)
                Box(
                    modifier = Modifier.align(Alignment.Center).size(240.dp).border(2.dp, MaterialTheme.colorScheme.primary)
                )
            } else {
                Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Camera permission is needed to scan barcodes.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) { Text("Grant Permission") }
                }
            }
        }
    }

    val notFound = result as? BarcodeResult.NotFound
    if (notFound != null) {
        AlertDialog(
            onDismissRequest = viewModel::reset,
            title = { Text("Barcode not recognized") },
            text = {
                Column {
                    Text("Barcode: ${notFound.barcode}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Link it to an existing product, or create a new one.")
                    Spacer(modifier = Modifier.height(8.dp))
                    var selectedProductId by remember { mutableStateOf<Long?>(null) }
                    ProductPickerField(products = products, selectedProductId = selectedProductId, onSelect = { selectedProductId = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { selectedProductId?.let { viewModel.linkToExistingProduct(notFound.barcode, it, onProductFound) } },
                        enabled = selectedProductId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Link to Selected Product") }
                }
            },
            confirmButton = {
                TextButton(onClick = { onCreateNewProduct(notFound.barcode) }) { Text("Create New Product") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::reset) { Text("Scan Again") }
            }
        )
    }
}

@Composable
private fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, BarcodeAnalyzer(onBarcodeDetected)) }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (_: Exception) {
                    // Camera unavailable (e.g. emulator without one) - preview stays blank.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
