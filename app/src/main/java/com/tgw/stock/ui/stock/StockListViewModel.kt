package com.tgw.stock.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.repository.CategoryRepository
import com.tgw.stock.data.repository.ProductListItem
import com.tgw.stock.data.repository.ProductRepository
import com.tgw.stock.data.repository.StockRepository
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class StockFilter { ALL, LOW, CRITICAL, OUT_OF_STOCK, EXPIRING_SOON }

class StockListViewModel(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val filter = MutableStateFlow(StockFilter.ALL)
    val categoryFilter = MutableStateFlow<Long?>(null)
    private val expiringSoonIds = MutableStateFlow<Set<Long>>(emptySet())

    val categories = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val baseItems: Flow<List<ProductListItem>> = combine(
        productRepository.observeAll(),
        categoryRepository.observeAll()
    ) { products, categories ->
        val categoryNames = categories.associateBy({ it.id }, { it.name })
        products.map { p ->
            ProductListItem(
                id = p.id,
                name = p.name,
                categoryId = p.categoryId,
                categoryName = p.categoryId?.let { categoryNames[it] },
                unit = p.unit,
                currentStock = p.currentStock,
                minStock = p.minStock,
                targetStock = p.targetStock,
                stockValueMinor = StockCalculations.stockValue(p.currentStock, p.avgCostPerUnitMinor).cents,
                status = StockCalculations.stockStatus(p.currentStock, p.minStock),
                barcode = p.barcode
            )
        }
    }

    val visibleItems: StateFlow<List<ProductListItem>> = combine(
        baseItems, searchQuery, filter, categoryFilter, expiringSoonIds
    ) { items, query, f, catId, expiring ->
        items.filter { item ->
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
            val matchesCategory = catId == null || item.categoryId == catId
            val matchesFilter = when (f) {
                StockFilter.ALL -> true
                StockFilter.LOW -> item.status == StockStatus.LOW
                StockFilter.CRITICAL -> item.status == StockStatus.CRITICAL
                StockFilter.OUT_OF_STOCK -> item.status == StockStatus.OUT_OF_STOCK
                StockFilter.EXPIRING_SOON -> expiring.contains(item.id)
            }
            matchesQuery && matchesCategory && matchesFilter
        }.sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshExpiring()
    }

    fun refreshExpiring() {
        viewModelScope.launch {
            expiringSoonIds.value = stockRepository.getExpiringSoon(3).map { it.productId }.toSet()
        }
    }
}
