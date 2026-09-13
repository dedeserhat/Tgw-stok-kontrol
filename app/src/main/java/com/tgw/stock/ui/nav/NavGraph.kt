package com.tgw.stock.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tgw.stock.ui.barcode.BarcodeScanScreen
import com.tgw.stock.ui.dashboard.DashboardScreen
import com.tgw.stock.ui.movements.StockCountDetailScreen
import com.tgw.stock.ui.movements.StockCountListScreen
import com.tgw.stock.ui.movements.StockInScreen
import com.tgw.stock.ui.movements.StockOutScreen
import com.tgw.stock.ui.purchase.PurchaseListScreen
import com.tgw.stock.ui.purchase.PurchaseOrderDetailScreen
import com.tgw.stock.ui.purchase.PurchaseOrderListScreen
import com.tgw.stock.ui.purchase.PurchaseOrderNewScreen
import com.tgw.stock.ui.recipes.RecipeAddScreen
import com.tgw.stock.ui.recipes.RecipeDetailScreen
import com.tgw.stock.ui.recipes.RecipeListScreen
import com.tgw.stock.ui.reports.ReportsScreen
import com.tgw.stock.ui.settings.SettingsScreen
import com.tgw.stock.ui.stock.ProductDetailScreen
import com.tgw.stock.ui.stock.ProductFormScreen
import com.tgw.stock.ui.stock.StockListScreen
import com.tgw.stock.ui.suppliers.SupplierDetailScreen
import com.tgw.stock.ui.suppliers.SupplierFormScreen
import com.tgw.stock.ui.suppliers.SupplierListScreen
import com.tgw.stock.ui.suppliers.SupplierPricesScreen
import com.tgw.stock.ui.waste.WasteScreen

private val bottomNavRoutes = setOf(Routes.DASHBOARD, Routes.STOCK_LIST, Routes.PURCHASE_LIST, Routes.RECIPE_LIST, Routes.MORE)

@Composable
fun TgwStockNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = backStackEntry?.destination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = { if (showBottomBar) TgwBottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToStock = { navController.navigate(Routes.STOCK_LIST) },
                    onNavigateToStockIn = { navController.navigate(Routes.stockIn()) },
                    onNavigateToStockOut = { navController.navigate(Routes.stockOut()) },
                    onNavigateToStockCount = { navController.navigate(Routes.STOCK_COUNT_LIST) },
                    onNavigateToAddProduct = { navController.navigate(Routes.productAdd()) },
                    onNavigateToNewPurchaseOrder = { navController.navigate(Routes.purchaseOrderNew()) },
                    onNavigateToPurchaseList = { navController.navigate(Routes.PURCHASE_LIST) },
                    onNavigateToBarcodeScan = { navController.navigate(Routes.BARCODE_SCAN) }
                )
            }

            composable(Routes.STOCK_LIST) {
                StockListScreen(
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onAddProduct = { navController.navigate(Routes.productAdd()) }
                )
            }

            composable(Routes.PRODUCT_DETAIL, arguments = listOf(navArgument("productId") { type = NavType.LongType })) { entry ->
                val productId = entry.arguments!!.getLong("productId")
                ProductDetailScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.productEdit(it)) },
                    onStockIn = { navController.navigate(Routes.stockIn(it)) },
                    onStockOut = { navController.navigate(Routes.stockOut(it)) },
                    onViewSupplierPrices = { navController.navigate(Routes.supplierPrices(it)) },
                    onRecipeClick = { navController.navigate(Routes.recipeDetail(it)) }
                )
            }

            composable(Routes.PRODUCT_EDIT, arguments = listOf(navArgument("productId") { type = NavType.LongType })) { entry ->
                val productId = entry.arguments!!.getLong("productId")
                ProductFormScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                Routes.PRODUCT_ADD,
                arguments = listOf(navArgument("barcode") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                val barcode = entry.arguments?.getString("barcode")
                ProductFormScreen(
                    productId = null,
                    initialBarcode = barcode,
                    onBack = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.productDetail(id))
                    }
                )
            }

            composable(
                Routes.STOCK_IN,
                arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val productId = entry.arguments!!.getLong("productId").takeIf { it >= 0 }
                StockInScreen(preselectedProductId = productId, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
            }

            composable(
                Routes.STOCK_OUT,
                arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val productId = entry.arguments!!.getLong("productId").takeIf { it >= 0 }
                StockOutScreen(preselectedProductId = productId, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
            }

            composable(Routes.STOCK_COUNT_LIST) {
                StockCountListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCount = { navController.navigate(Routes.stockCountDetail(it)) }
                )
            }

            composable(Routes.STOCK_COUNT_DETAIL, arguments = listOf(navArgument("countId") { type = NavType.LongType })) { entry ->
                StockCountDetailScreen(countId = entry.arguments!!.getLong("countId"), onBack = { navController.popBackStack() })
            }

            composable(Routes.RECIPE_LIST) {
                RecipeListScreen(
                    onRecipeClick = { navController.navigate(Routes.recipeDetail(it)) },
                    onAddRecipe = { navController.navigate(Routes.RECIPE_ADD) }
                )
            }

            composable(Routes.RECIPE_DETAIL, arguments = listOf(navArgument("recipeId") { type = NavType.LongType })) { entry ->
                RecipeDetailScreen(recipeId = entry.arguments!!.getLong("recipeId"), onBack = { navController.popBackStack() })
            }

            composable(Routes.RECIPE_ADD) {
                RecipeAddScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { id -> navController.popBackStack(); navController.navigate(Routes.recipeDetail(id)) }
                )
            }

            composable(Routes.SUPPLIER_LIST) {
                SupplierListScreen(
                    onBack = { navController.popBackStack() },
                    onSupplierClick = { navController.navigate(Routes.supplierDetail(it)) },
                    onAddSupplier = { navController.navigate(Routes.SUPPLIER_ADD) }
                )
            }

            composable(Routes.SUPPLIER_DETAIL, arguments = listOf(navArgument("supplierId") { type = NavType.LongType })) { entry ->
                val supplierId = entry.arguments!!.getLong("supplierId")
                SupplierDetailScreen(
                    supplierId = supplierId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.supplierEdit(it)) }
                )
            }

            composable(Routes.SUPPLIER_ADD) {
                SupplierFormScreen(
                    supplierId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { id -> navController.popBackStack(); navController.navigate(Routes.supplierDetail(id)) }
                )
            }

            composable(Routes.SUPPLIER_EDIT, arguments = listOf(navArgument("supplierId") { type = NavType.LongType })) { entry ->
                SupplierFormScreen(
                    supplierId = entry.arguments!!.getLong("supplierId"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.SUPPLIER_PRICES, arguments = listOf(navArgument("productId") { type = NavType.LongType })) { entry ->
                SupplierPricesScreen(productId = entry.arguments!!.getLong("productId"), onBack = { navController.popBackStack() })
            }

            composable(Routes.PURCHASE_LIST) {
                PurchaseListScreen(
                    onBack = { navController.popBackStack() },
                    onViewOrders = { navController.navigate(Routes.PURCHASE_ORDER_LIST) },
                    onNewManualOrder = { navController.navigate(Routes.purchaseOrderNew()) },
                    onOrderCreated = { id -> navController.navigate(Routes.purchaseOrderDetail(id)) }
                )
            }

            composable(Routes.PURCHASE_ORDER_LIST) {
                PurchaseOrderListScreen(onBack = { navController.popBackStack() }, onOrderClick = { navController.navigate(Routes.purchaseOrderDetail(it)) })
            }

            composable(Routes.PURCHASE_ORDER_DETAIL, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) { entry ->
                PurchaseOrderDetailScreen(orderId = entry.arguments!!.getLong("orderId"), onBack = { navController.popBackStack() })
            }

            composable(
                Routes.PURCHASE_ORDER_NEW,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val supplierId = entry.arguments!!.getLong("supplierId").takeIf { it >= 0 }
                PurchaseOrderNewScreen(
                    preselectedSupplierId = supplierId,
                    onBack = { navController.popBackStack() },
                    onCreated = { id -> navController.popBackStack(); navController.navigate(Routes.purchaseOrderDetail(id)) }
                )
            }

            composable(Routes.WASTE) { WasteScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.REPORTS) { ReportsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }

            composable(Routes.MORE) {
                MoreScreen(
                    onSuppliers = { navController.navigate(Routes.SUPPLIER_LIST) },
                    onPurchaseOrders = { navController.navigate(Routes.PURCHASE_ORDER_LIST) },
                    onStockCounts = { navController.navigate(Routes.STOCK_COUNT_LIST) },
                    onWaste = { navController.navigate(Routes.WASTE) },
                    onReports = { navController.navigate(Routes.REPORTS) },
                    onSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.BARCODE_SCAN) {
                BarcodeScanScreen(
                    onBack = { navController.popBackStack() },
                    onProductFound = { id -> navController.popBackStack(); navController.navigate(Routes.productDetail(id)) },
                    onCreateNewProduct = { barcode -> navController.popBackStack(); navController.navigate(Routes.productAdd(barcode)) }
                )
            }
        }
    }
}
