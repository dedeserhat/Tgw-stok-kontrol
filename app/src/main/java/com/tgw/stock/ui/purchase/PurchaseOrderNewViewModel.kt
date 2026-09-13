package com.tgw.stock.ui.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.PurchaseRepository
import com.tgw.stock.data.repository.SupplierPriceRepository
import com.tgw.stock.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DraftLine(val productId: Long, val productName: String, val quantity: Double, val unitCostMinor: Long)

class PurchaseOrderNewViewModel(
    preselectedSupplierId: Long?,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierPriceRepository: SupplierPriceRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    val suppliers = MutableStateFlow<List<SupplierEntity>>(emptyList())
    val products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val supplierId = MutableStateFlow(preselectedSupplierId)
    val lines = MutableStateFlow<List<DraftLine>>(emptyList())

    private val _createdOrderId = MutableStateFlow<Long?>(null)
    val createdOrderId: StateFlow<Long?> = _createdOrderId.asStateFlow()

    init {
        viewModelScope.launch {
            suppliers.value = supplierRepository.getAll()
            products.value = productRepository.getAll()
        }
    }

    fun selectSupplier(id: Long) { supplierId.value = id }

    fun addLine(productId: Long, quantity: Double) {
        viewModelScope.launch {
            val product = products.value.firstOrNull { it.id == productId } ?: return@launch
            val cheapest = supplierPriceRepository.getCheapestOffer(productId)
            val unitCost = cheapest?.pricePerBaseUnitMinor ?: product.lastCostPerUnitMinor
            lines.value = lines.value + DraftLine(productId, product.name, quantity, unitCost)
        }
    }

    fun removeLine(index: Int) {
        lines.value = lines.value.toMutableList().apply { removeAt(index) }
    }

    fun submit() {
        val supplier = supplierId.value ?: return
        if (lines.value.isEmpty()) return
        viewModelScope.launch {
            val costByProduct = lines.value.associate { it.productId to it.unitCostMinor }
            val id = purchaseRepository.createOrder(
                supplierId = supplier,
                items = lines.value.map { it.productId to it.quantity },
                priceLookup = { productId -> costByProduct[productId] ?: 0L }
            )
            _createdOrderId.value = id
        }
    }
}
