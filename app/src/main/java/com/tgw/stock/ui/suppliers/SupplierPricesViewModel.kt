package com.tgw.stock.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.local.entities.SupplierPriceEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.SupplierOffer
import com.tgw.stock.data.repository.SupplierPriceRepository
import com.tgw.stock.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SupplierPricesViewModel(
    private val productId: Long,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierPriceRepository: SupplierPriceRepository
) : ViewModel() {

    private val _product = MutableStateFlow<ProductEntity?>(null)
    val product: StateFlow<ProductEntity?> = _product.asStateFlow()

    private val _offers = MutableStateFlow<List<SupplierOffer>>(emptyList())
    val offers: StateFlow<List<SupplierOffer>> = _offers.asStateFlow()

    val allSuppliers = MutableStateFlow<List<SupplierEntity>>(emptyList())

    private val _historyBySupplierProduct = MutableStateFlow<Map<Long, List<SupplierPriceEntity>>>(emptyMap())
    val historyBySupplierProduct: StateFlow<Map<Long, List<SupplierPriceEntity>>> = _historyBySupplierProduct.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _product.value = productRepository.getById(productId)
            allSuppliers.value = supplierRepository.getAll()
            val offers = supplierPriceRepository.getOffersForProduct(productId)
            _offers.value = offers.sortedBy { it.pricePerBaseUnitMinor }
            val history = mutableMapOf<Long, List<SupplierPriceEntity>>()
            for (offer in offers) {
                history[offer.supplierProductId] = supplierPriceRepository.observePriceHistory(offer.supplierProductId).first()
            }
            _historyBySupplierProduct.value = history
        }
    }

    fun addOffer(supplierId: Long, packageDescription: String, packageQuantity: Double, priceMinor: Long, effectiveDate: Long) {
        viewModelScope.launch {
            supplierPriceRepository.addOrUpdateOffer(supplierId, productId, packageDescription, packageQuantity, priceMinor, effectiveDate)
            refresh()
        }
    }

    fun addPriceUpdate(supplierProductId: Long, priceMinor: Long, effectiveDate: Long) {
        viewModelScope.launch {
            val link = supplierPriceRepository.getSupplierProduct(supplierProductId) ?: return@launch
            supplierPriceRepository.addOrUpdateOffer(
                link.supplierId, link.productId, link.packageDescription, link.packageQuantity, priceMinor, effectiveDate, supplierProductId
            )
            refresh()
        }
    }
}
