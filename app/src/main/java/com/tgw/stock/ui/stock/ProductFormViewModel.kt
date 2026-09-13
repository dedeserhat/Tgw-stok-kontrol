package com.tgw.stock.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.CategoryEntity
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.CategoryRepository
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.SupplierRepository
import com.tgw.stock.domain.StockUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductFormState(
    val name: String = "",
    val categoryId: Long? = null,
    val unit: StockUnit = StockUnit.KG,
    val minStock: String = "",
    val targetStock: String = "",
    val barcode: String = "",
    val preferredSupplierId: Long? = null,
    val nameError: String? = null,
    val minStockError: String? = null,
    val targetStockError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val savedProductId: Long? = null
)

class ProductFormViewModel(
    private val productId: Long?,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val supplierRepository: SupplierRepository,
    initialBarcode: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFormState(isEditMode = productId != null, barcode = initialBarcode ?: ""))
    val state: StateFlow<ProductFormState> = _state.asStateFlow()

    val categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val suppliers = MutableStateFlow<List<SupplierEntity>>(emptyList())

    init {
        viewModelScope.launch {
            categories.value = categoryRepository.getAll()
            suppliers.value = supplierRepository.getAll()
            if (productId != null) {
                productRepository.getById(productId)?.let { p ->
                    _state.value = ProductFormState(
                        name = p.name,
                        categoryId = p.categoryId,
                        unit = StockUnit.fromNameSafe(p.unit),
                        minStock = p.minStock.toString(),
                        targetStock = p.targetStock.toString(),
                        barcode = p.barcode ?: "",
                        preferredSupplierId = p.preferredSupplierId,
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun updateName(value: String) { _state.value = _state.value.copy(name = value, nameError = null) }
    fun updateCategory(id: Long?) { _state.value = _state.value.copy(categoryId = id) }
    fun updateUnit(unit: StockUnit) { _state.value = _state.value.copy(unit = unit) }
    fun updateMinStock(value: String) { _state.value = _state.value.copy(minStock = value, minStockError = null) }
    fun updateTargetStock(value: String) { _state.value = _state.value.copy(targetStock = value, targetStockError = null) }
    fun updateBarcode(value: String) { _state.value = _state.value.copy(barcode = value) }
    fun updateSupplier(id: Long?) { _state.value = _state.value.copy(preferredSupplierId = id) }

    fun save() {
        val s = _state.value
        val minStock = s.minStock.toDoubleOrNull()
        val targetStock = s.targetStock.toDoubleOrNull()

        var hasError = false
        var nameError: String? = null
        var minError: String? = null
        var targetError: String? = null

        if (s.name.isBlank()) { nameError = "Name is required"; hasError = true }
        if (minStock == null || minStock < 0.0) { minError = "Enter a valid minimum"; hasError = true }
        if (targetStock == null || targetStock < 0.0) { targetError = "Enter a valid target"; hasError = true }
        if (minStock != null && targetStock != null && targetStock < minStock) {
            targetError = "Target must be at least the minimum"; hasError = true
        }

        if (hasError) {
            _state.value = s.copy(nameError = nameError, minStockError = minError, targetStockError = targetError)
            return
        }

        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val existing = productId?.let { productRepository.getById(it) }
            val entity = ProductEntity(
                id = productId ?: 0,
                name = s.name.trim(),
                categoryId = s.categoryId,
                unit = s.unit.name,
                currentStock = existing?.currentStock ?: 0.0,
                minStock = minStock!!,
                targetStock = targetStock!!,
                avgCostPerUnitMinor = existing?.avgCostPerUnitMinor ?: 0,
                lastCostPerUnitMinor = existing?.lastCostPerUnitMinor ?: 0,
                lastPurchaseDate = existing?.lastPurchaseDate,
                preferredSupplierId = s.preferredSupplierId,
                barcode = s.barcode.trim().ifBlank { null },
                isSample = existing?.isSample ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val id = productRepository.upsert(entity)
            _state.value = _state.value.copy(isSaving = false, savedProductId = if (productId != null) productId else id)
        }
    }
}
