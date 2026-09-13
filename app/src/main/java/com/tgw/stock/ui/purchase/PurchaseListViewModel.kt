package com.tgw.stock.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.repository.PurchaseRepository
import com.tgw.stock.data.repository.SupplierPurchaseGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PurchaseListViewModel(private val purchaseRepository: PurchaseRepository) : ViewModel() {

    private val _groups = MutableStateFlow<List<SupplierPurchaseGroup>>(emptyList())
    val groups: StateFlow<List<SupplierPurchaseGroup>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _createdOrderId = MutableStateFlow<Long?>(null)
    val createdOrderId: StateFlow<Long?> = _createdOrderId.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _groups.value = purchaseRepository.buildPurchaseList()
            _isLoading.value = false
        }
    }

    fun createOrderForGroup(group: SupplierPurchaseGroup) {
        if (group.supplierId < 0) return // "No supplier set" group can't become a PO
        viewModelScope.launch {
            val priceBySupplierProduct = group.lines.associate { it.productId to (it.bestOffer?.pricePerBaseUnitMinor ?: 0L) }
            val id = purchaseRepository.createOrder(
                supplierId = group.supplierId,
                items = group.lines.map { it.productId to it.quantityToBuy },
                priceLookup = { productId -> priceBySupplierProduct[productId] ?: 0L }
            )
            _createdOrderId.value = id
        }
    }

    fun consumeCreatedOrderId() { _createdOrderId.value = null }
}
