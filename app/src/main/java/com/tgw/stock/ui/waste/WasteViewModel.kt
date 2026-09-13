package com.tgw.stock.ui.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.ProductEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.WasteRepository
import com.tgw.stock.domain.WasteReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WasteRow(val productName: String, val unit: String, val quantity: Double, val reason: String, val costMinor: Long, val note: String?, val recordedDate: Long)

class WasteViewModel(
    private val wasteRepository: WasteRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    val products = MutableStateFlow<List<ProductEntity>>(emptyList())

    private val _rows = MutableStateFlow<List<WasteRow>>(emptyList())
    val rows: StateFlow<List<WasteRow>> = _rows.asStateFlow()

    private val _weeklyTotal = MutableStateFlow(0L)
    val weeklyTotal: StateFlow<Long> = _weeklyTotal.asStateFlow()

    private val _monthlyTotal = MutableStateFlow(0L)
    val monthlyTotal: StateFlow<Long> = _monthlyTotal.asStateFlow()

    init {
        viewModelScope.launch {
            products.value = productRepository.getAll()
            wasteRepository.observeAll().collect { records ->
                val productsById = productRepository.getAllIncludingArchived().associateBy { it.id }
                _rows.value = records.map { r ->
                    val p = productsById[r.productId]
                    WasteRow(p?.name ?: "Unknown", p?.unit ?: "", r.quantity, r.reason, r.totalCostMinor, r.note, r.recordedDate)
                }
                _weeklyTotal.value = wasteRepository.getWeeklyTotalCost()
                _monthlyTotal.value = wasteRepository.getMonthlyTotalCost()
            }
        }
    }

    fun recordWaste(productId: Long, quantity: Double, reason: WasteReason, note: String?) {
        viewModelScope.launch {
            wasteRepository.recordWaste(productId, quantity, reason, note)
        }
    }
}
