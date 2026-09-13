package com.tgw.stock.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.SupplierOffer
import com.tgw.stock.data.repository.SupplierPriceRepository
import com.tgw.stock.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupplierDetailViewModel(
    private val supplierId: Long,
    private val supplierRepository: SupplierRepository,
    private val supplierPriceRepository: SupplierPriceRepository
) : ViewModel() {

    private val _supplier = MutableStateFlow<SupplierEntity?>(null)
    val supplier: StateFlow<SupplierEntity?> = _supplier.asStateFlow()

    private val _offers = MutableStateFlow<List<Pair<String, SupplierOffer>>>(emptyList())
    val offers: StateFlow<List<Pair<String, SupplierOffer>>> = _offers.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _supplier.value = supplierRepository.getById(supplierId)
            _offers.value = supplierPriceRepository.getOffersForSupplier(supplierId)
        }
    }

    fun deleteSupplier() {
        viewModelScope.launch {
            _supplier.value?.let { supplierRepository.delete(it) }
            _deleted.value = true
        }
    }
}
