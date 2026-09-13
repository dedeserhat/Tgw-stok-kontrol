package com.tgw.stock.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.StockRepository
import com.tgw.stock.data.repository.SupplierRepository
import com.tgw.stock.domain.Money
import com.tgw.stock.domain.StockInReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockInFormState(
    val products: List<ProductEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val productId: Long? = null,
    val quantity: String = "",
    val unitCost: String = "",
    val reason: StockInReason = StockInReason.SUPPLIER_DELIVERY,
    val supplierId: Long? = null,
    val expiryDate: Long? = null,
    val note: String = "",
    val quantityError: String? = null,
    val unitCostError: String? = null,
    val productError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class StockInViewModel(
    preselectedProductId: Long?,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StockInFormState(productId = preselectedProductId))
    val state: StateFlow<StockInFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                products = productRepository.getAll(),
                suppliers = supplierRepository.getAll()
            )
            preselectedProductId?.let { id ->
                productRepository.getById(id)?.let { p ->
                    _state.value = _state.value.copy(supplierId = p.preferredSupplierId)
                }
            }
        }
    }

    fun selectProduct(id: Long) {
        _state.value = _state.value.copy(productId = id, productError = null)
        viewModelScope.launch {
            productRepository.getById(id)?.let { p ->
                if (_state.value.supplierId == null) {
                    _state.value = _state.value.copy(supplierId = p.preferredSupplierId)
                }
            }
        }
    }

    fun updateQuantity(v: String) { _state.value = _state.value.copy(quantity = v, quantityError = null) }
    fun updateUnitCost(v: String) { _state.value = _state.value.copy(unitCost = v, unitCostError = null) }
    fun updateReason(r: StockInReason) { _state.value = _state.value.copy(reason = r) }
    fun updateSupplier(id: Long?) { _state.value = _state.value.copy(supplierId = id) }
    fun updateExpiryDate(millis: Long?) { _state.value = _state.value.copy(expiryDate = millis) }
    fun updateNote(v: String) { _state.value = _state.value.copy(note = v) }

    fun save() {
        val s = _state.value
        val quantity = s.quantity.toDoubleOrNull()
        val unitCost = Money.parseOrNull(s.unitCost)

        var hasError = false
        var productError: String? = null
        var quantityError: String? = null
        var unitCostError: String? = null

        if (s.productId == null) { productError = "Select a product"; hasError = true }
        if (quantity == null || quantity <= 0.0) { quantityError = "Enter a valid quantity"; hasError = true }
        if (unitCost == null) { unitCostError = "Enter a valid cost"; hasError = true }

        if (hasError) {
            _state.value = s.copy(productError = productError, quantityError = quantityError, unitCostError = unitCostError)
            return
        }

        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            stockRepository.stockIn(
                productId = s.productId!!,
                quantity = quantity!!,
                unitCostMinor = unitCost!!.cents,
                reason = s.reason,
                note = s.note.trim().ifBlank { null },
                expiryDate = s.expiryDate,
                supplierId = s.supplierId
            )
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
