package com.tgw.stock.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.StockBatchEntity
import com.tgw.stock.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PriceHistoryEntry(
    val supplierName: String,
    val packageDescription: String,
    val priceMinor: Long,
    val effectiveDate: Long
)

class ProductDetailViewModel(
    private val productId: Long,
    private val productRepository: ProductRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val supplierPriceRepository: SupplierPriceRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<ProductDetail?>(null)
    val detail: StateFlow<ProductDetail?> = _detail.asStateFlow()

    private val _priceHistory = MutableStateFlow<List<PriceHistoryEntry>>(emptyList())
    val priceHistory: StateFlow<List<PriceHistoryEntry>> = _priceHistory.asStateFlow()

    private val _offers = MutableStateFlow<List<SupplierOffer>>(emptyList())
    val offers: StateFlow<List<SupplierOffer>> = _offers.asStateFlow()

    val batches: StateFlow<List<StockBatchEntity>> = stockRepository.observeBatchesForProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _detail.value = productDetailRepository.getDetail(productId)
            val offers = supplierPriceRepository.getOffersForProduct(productId)
            _offers.value = offers
            val history = mutableListOf<PriceHistoryEntry>()
            for (offer in offers) {
                supplierPriceRepository.observePriceHistory(offer.supplierProductId).first().forEach { price ->
                    history.add(PriceHistoryEntry(offer.supplierName, offer.packageDescription, price.priceMinor, price.effectiveDate))
                }
            }
            _priceHistory.value = history.sortedByDescending { it.effectiveDate }
        }
    }

    suspend fun deleteProduct() {
        productRepository.archive(productId)
    }
}
