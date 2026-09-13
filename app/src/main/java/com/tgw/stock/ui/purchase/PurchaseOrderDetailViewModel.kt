package com.tgw.stock.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.PurchaseOrderEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.PurchaseRepository
import com.tgw.stock.data.repository.SupplierRepository
import com.tgw.stock.domain.PurchaseOrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseOrderItemRow(
    val itemId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitCostMinor: Long,
    val receivedQuantity: Double
)

class PurchaseOrderDetailViewModel(
    private val orderId: Long,
    private val purchaseRepository: PurchaseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _order = MutableStateFlow<PurchaseOrderEntity?>(null)
    val order: StateFlow<PurchaseOrderEntity?> = _order.asStateFlow()

    val supplierName = MutableStateFlow("")

    private val _rows = MutableStateFlow<List<PurchaseOrderItemRow>>(emptyList())
    val rows: StateFlow<List<PurchaseOrderItemRow>> = _rows.asStateFlow()

    init {
        viewModelScope.launch {
            purchaseRepository.observeOrder(orderId).collect { order ->
                _order.value = order
                if (order != null) {
                    supplierName.value = supplierRepository.getById(order.supplierId)?.companyName ?: "Unknown"
                    reloadItems()
                }
            }
        }
    }

    private suspend fun reloadItems() {
        val items = purchaseRepository.getOrderItems(orderId)
        val products = productRepository.getAll().associateBy { it.id }
        _rows.value = items.map { item ->
            val product = products[item.productId]
            PurchaseOrderItemRow(
                itemId = item.id,
                productId = item.productId,
                productName = product?.name ?: "Unknown",
                unit = product?.unit ?: "",
                quantity = item.quantity,
                unitCostMinor = item.unitCostMinor,
                receivedQuantity = item.receivedQuantity
            )
        }
    }

    fun markOrdered() = updateStatus(PurchaseOrderStatus.ORDERED)
    fun markCancelled() = updateStatus(PurchaseOrderStatus.CANCELLED)

    private fun updateStatus(status: PurchaseOrderStatus) {
        viewModelScope.launch {
            purchaseRepository.updateOrderStatus(orderId, status)
        }
    }

    fun receiveStock(receivedQuantities: Map<Long, Double>, expiryDates: Map<Long, Long?>) {
        viewModelScope.launch {
            purchaseRepository.receiveOrder(orderId, receivedQuantities, expiryDates)
            reloadItems()
        }
    }
}
