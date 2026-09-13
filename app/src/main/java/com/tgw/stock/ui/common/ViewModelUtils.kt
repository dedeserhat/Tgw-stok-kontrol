package com.tgw.stock.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tgw.stock.TgwStockApp
import com.tgw.stock.di.AppContainer

/** Thin wrapper around the no-Hilt viewModelFactory DSL so every screen can request
 * a ViewModel with `tgwViewModel { MyViewModel(container.someRepository) }` instead of
 * repeating the factory boilerplate. */
@Composable
inline fun <reified T : ViewModel> tgwViewModel(crossinline create: (AppContainer) -> T): T {
    val app = LocalContext.current.applicationContext as TgwStockApp
    val factory = viewModelFactory {
        initializer { create(app.container) }
    }
    return viewModel(factory = factory)
}

fun appContainer(context: android.content.Context): AppContainer =
    (context.applicationContext as TgwStockApp).container
