package com.tgw.stock.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.repository.PurchaseRepository
import com.tgw.stock.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseOrderRow(
    val id: Long,
    val poNumber: String,
    val supplierName: String,
    val status: String,
    val createdAt: Long
)

class PurchaseOrderListViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _rows = MutableStateFlow<List<PurchaseOrderRow>>(emptyList())
    val rows: StateFlow<List<PurchaseOrderRow>> = _rows.asStateFlow()

    init {
        viewModelScope.launch {
            purchaseRepository.observeOrders().collect { orders ->
                val suppliers = supplierRepository.getAll().associateBy { it.id }
                _rows.value = orders.map {
                    PurchaseOrderRow(it.id, it.poNumber, suppliers[it.supplierId]?.companyName ?: "Unknown", it.status, it.createdAt)
                }
            }
        }
    }
}
