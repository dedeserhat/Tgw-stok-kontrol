package com.tgw.stock.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tgw.stock.data.local.entities.ProductEntity

/** Searchable dropdown used anywhere a screen needs to pick one product from a
 * potentially long list (stock in/out, waste, recipe editor, purchase orders). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPickerField(
    products: List<ProductEntity>,
    selectedProductId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Product",
    isError: Boolean = false,
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selectedProductId) {
        mutableStateOf(products.firstOrNull { it.id == selectedProductId }?.name ?: "")
    }
    val filtered = remember(query, products) {
        if (query.isBlank()) products else products.filter { it.name.contains(query, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            label = { Text(label) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(text = { Text("No matches") }, onClick = {}, enabled = false)
            }
            filtered.take(50).forEach { product ->
                DropdownMenuItem(
                    text = { Text(product.name) },
                    onClick = {
                        query = product.name
                        expanded = false
                        onSelect(product.id)
                    }
                )
            }
        }
    }
}
