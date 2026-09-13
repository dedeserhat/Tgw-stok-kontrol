package com.tgw.stock.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgw.stock.data.local.entities.SupplierEntity
import com.tgw.stock.data.repository.SupplierRepository
import com.tgw.stock.domain.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

data class SupplierFormState(
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val minOrder: String = "",
    val selectedDays: Set<String> = emptySet(),
    val paymentTerms: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val savedId: Long? = null,
    val isEditMode: Boolean = false
)

class SupplierFormViewModel(
    private val supplierId: Long?,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierFormState(isEditMode = supplierId != null))
    val state: StateFlow<SupplierFormState> = _state.asStateFlow()

    init {
        if (supplierId != null) {
            viewModelScope.launch {
                supplierRepository.getById(supplierId)?.let { s ->
                    _state.value = SupplierFormState(
                        companyName = s.companyName,
                        phone = s.phone ?: "",
                        email = s.email ?: "",
                        address = s.address ?: "",
                        minOrder = s.minOrderMinor?.let { (it / 100.0).toString() } ?: "",
                        selectedDays = s.deliveryDays?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                        paymentTerms = s.paymentTerms ?: "",
                        notes = s.notes ?: "",
                        isEditMode = true
                    )
                }
            }
        }
    }

    fun updateField(update: (SupplierFormState) -> SupplierFormState) { _state.value = update(_state.value) }

    fun toggleDay(day: String) {
        val current = _state.value.selectedDays
        _state.value = _state.value.copy(selectedDays = if (day in current) current - day else current + day)
    }

    fun save() {
        val s = _state.value
        if (s.companyName.isBlank()) {
            _state.value = s.copy(nameError = "Company name is required")
            return
        }
        _state.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val existing = supplierId?.let { supplierRepository.getById(it) }
            val entity = SupplierEntity(
                id = supplierId ?: 0,
                companyName = s.companyName.trim(),
                phone = s.phone.trim().ifBlank { null },
                email = s.email.trim().ifBlank { null },
                address = s.address.trim().ifBlank { null },
                minOrderMinor = Money.parseOrNull(s.minOrder)?.cents,
                deliveryDays = s.selectedDays.takeIf { it.isNotEmpty() }?.joinToString(","),
                paymentTerms = s.paymentTerms.trim().ifBlank { null },
                notes = s.notes.trim().ifBlank { null },
                isSample = existing?.isSample ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val id = supplierRepository.upsert(entity)
            _state.value = _state.value.copy(isSaving = false, savedId = if (supplierId != null) supplierId else id)
        }
    }
}
