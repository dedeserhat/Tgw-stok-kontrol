package com.tgw.stock.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BarcodeResult {
    data object Scanning : BarcodeResult()
    data class Found(val productId: Long) : BarcodeResult()
    data class NotFound(val barcode: String) : BarcodeResult()
}

class BarcodeScanViewModel(private val productRepository: ProductRepository) : ViewModel() {

    private val _result = MutableStateFlow<BarcodeResult>(BarcodeResult.Scanning)
    val result: StateFlow<BarcodeResult> = _result.asStateFlow()

    val allProducts = MutableStateFlow<List<ProductEntity>>(emptyList())

    private var isLookupInFlight = false

    init {
        viewModelScope.launch { allProducts.value = productRepository.getAll() }
    }

    fun onBarcodeDetected(barcode: String) {
        if (isLookupInFlight || _result.value != BarcodeResult.Scanning) return
        isLookupInFlight = true
        viewModelScope.launch {
            val product = productRepository.getByBarcode(barcode)
            _result.value = if (product != null) BarcodeResult.Found(product.id) else BarcodeResult.NotFound(barcode)
            isLookupInFlight = false
        }
    }

    fun linkToExistingProduct(barcode: String, productId: Long, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            productRepository.getById(productId)?.let { product ->
                productRepository.upsert(product.copy(barcode = barcode))
                onDone(productId)
            }
        }
    }

    fun reset() {
        _result.value = BarcodeResult.Scanning
    }
}
