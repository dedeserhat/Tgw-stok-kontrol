package com.tgw.stock.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.StockRepository
import com.tgw.stock.domain.StockOutReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockOutFormState(
    val products: List<ProductEntity> = emptyList(),
    val productId: Long? = null,
    val currentStock: Double? = null,
    val unit: String = "",
    val quantity: String = "",
    val reason: StockOutReason = StockOutReason.KITCHEN_USAGE,
    val note: String = "",
    val quantityError: String? = null,
    val productError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class StockOutViewModel(
    preselectedProductId: Long?,
    private val productRepository: ProductRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StockOutFormState(productId = preselectedProductId))
    val state: StateFlow<StockOutFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(products = productRepository.getAll())
            preselectedProductId?.let { loadCurrentStock(it) }
        }
    }

    private suspend fun loadCurrentStock(productId: Long) {
        productRepository.getById(productId)?.let { p ->
            _state.value = _state.value.copy(currentStock = p.currentStock, unit = p.unit)
        }
    }

    fun selectProduct(id: Long) {
        _state.value = _state.value.copy(productId = id, productError = null)
        viewModelScope.launch { loadCurrentStock(id) }
    }

    fun updateQuantity(v: String) { _state.value = _state.value.copy(quantity = v, quantityError = null) }
    fun updateReason(r: StockOutReason) { _state.value = _state.value.copy(reason = r) }
    fun updateNote(v: String) { _state.value = _state.value.copy(note = v) }

    fun save() {
        val s = _state.value
        val quantity = s.quantity.toDoubleOrNull()

        var hasError = false
        var productError: String? = null
        var quantityError: String? = null

        if (s.productId == null) { productError = "Select a product"; hasError = true }
        if (quantity == null || quantity <= 0.0) {
            quantityError = "Enter a valid quantity"; hasError = true
        } else if (s.currentStock != null && quantity > s.currentStock) {
            quantityError = "Only ${s.currentStock} ${s.unit} available"; hasError = true
        }

        if (hasError) {
            _state.value = s.copy(productError = productError, quantityError = quantityError)
            return
        }

        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            stockRepository.stockOut(
                productId = s.productId!!,
                quantity = quantity!!,
                reason = s.reason,
                note = s.note.trim().ifBlank { null }
            )
            _state.value = _state.value.copy(isSaving = false, saved = true)
        }
    }
}
