package com.tgw.stock.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.SupplierRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SupplierListViewModel(supplierRepository: SupplierRepository) : ViewModel() {
    val suppliers: StateFlow<List<SupplierEntity>> = supplierRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
