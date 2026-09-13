package com.tgw.stock.ui.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.StockCountEntity
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.StockCountRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockCountListViewModel(
    private val stockCountRepository: StockCountRepository
) : ViewModel() {
    val counts: StateFlow<List<StockCountEntity>> = stockCountRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newCountId = MutableStateFlow<Long?>(null)
    val newCountId: StateFlow<Long?> = _newCountId.asStateFlow()

    fun startNewCount() {
        viewModelScope.launch {
            _newCountId.value = stockCountRepository.startCount()
        }
    }

    fun consumeNewCountId() { _newCountId.value = null }
}

data class StockCountRow(
    val itemId: Long,
    val productId: Long,
    val productName: String,
    val unit: String,
    val systemQuantity: Double,
    val countedQuantity: Double?
) {
    val difference: Double? get() = countedQuantity?.let { it - systemQuantity }
}

class StockCountDetailViewModel(
    private val countId: Long,
    private val stockCountRepository: StockCountRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _count = MutableStateFlow<StockCountEntity?>(null)
    val count: StateFlow<StockCountEntity?> = _count.asStateFlow()

    private val _rows = MutableStateFlow<List<StockCountRow>>(emptyList())
    val rows: StateFlow<List<StockCountRow>> = _rows.asStateFlow()

    private val _isCommitting = MutableStateFlow(false)
    val isCommitting: StateFlow<Boolean> = _isCommitting.asStateFlow()

    init {
        viewModelScope.launch {
            _count.value = stockCountRepository.getById(countId)
            stockCountRepository.observeItems(countId).collect { items ->
                val products = productRepository.getAll().associateBy { it.id }
                _rows.value = items.map { item ->
                    val product = products[item.productId]
                    StockCountRow(
                        itemId = item.id,
                        productId = item.productId,
                        productName = product?.name ?: "Unknown",
                        unit = product?.unit ?: "",
                        systemQuantity = item.systemQuantity,
                        countedQuantity = item.countedQuantity
                    )
                }.sortedBy { it.productName }
            }
        }
    }

    fun updateCounted(itemId: Long, value: Double?) {
        viewModelScope.launch {
            stockCountRepository.updateCountedQuantity(itemId, value)
        }
    }

    fun commit(onDone: () -> Unit) {
        viewModelScope.launch {
            _isCommitting.value = true
            stockCountRepository.commitCount(countId)
            _isCommitting.value = false
            onDone()
        }
    }
}
