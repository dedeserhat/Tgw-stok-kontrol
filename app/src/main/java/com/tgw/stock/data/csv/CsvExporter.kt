package com.tgw.stock.data.csv

import android.content.Context
import com.tgw.stock.data.local.dao.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Exports the core tables as plain CSV files under cacheDir/exports so they can be
 * shared via a FileProvider share sheet (email, Drive, USB transfer, etc.). */
class CsvExporter(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val supplierProductDao: SupplierProductDao,
    private val supplierPriceDao: SupplierPriceDao,
    private val recipeDao: RecipeDao,
    private val recipeItemDao: RecipeItemDao,
    private val movementDao: StockMovementDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val purchaseOrderItemDao: PurchaseOrderItemDao,
    private val wasteDao: WasteRecordDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun csvEscape(value: Any?): String {
        val text = value?.toString() ?: ""
        return if (text.contains(',') || text.contains('"') || text.contains('\n')) {
            "\"${text.replace("\"", "\"\"")}\""
        } else text
    }

    private fun buildCsv(headers: List<String>, rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append(headers.joinToString(",") { csvEscape(it) }).append('\n')
        for (row in rows) sb.append(row.joinToString(",") { csvEscape(it) }).append('\n')
        return sb.toString()
    }

    private fun writeFile(name: String, content: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, name)
        file.writeText(content)
        return file
    }

    suspend fun exportAll(): List<File> = withContext(Dispatchers.IO) {
        listOf(
            exportProducts(),
            exportSuppliers(),
            exportSupplierPrices(),
            exportRecipes(),
            exportPurchaseOrders(),
            exportStockMovements(),
            exportWaste()
        )
    }

    private suspend fun exportProducts(): File {
        val categories = categoryDao.getAll().associateBy { it.id }
        val rows = productDao.getAll().map { p ->
            listOf(
                p.id, p.name, categories[p.categoryId]?.name ?: "", p.unit,
                p.currentStock, p.minStock, p.targetStock,
                p.avgCostPerUnitMinor / 100.0, p.lastCostPerUnitMinor / 100.0,
                p.lastPurchaseDate?.let { dateFormat.format(Date(it)) } ?: "", p.barcode ?: ""
            )
        }
        return writeFile(
            "products.csv",
            buildCsv(
                listOf("id", "name", "category", "unit", "current_stock", "min_stock", "target_stock", "avg_cost", "last_cost", "last_purchase_date", "barcode"),
                rows
            )
        )
    }

    private suspend fun exportSuppliers(): File {
        val rows = supplierDao.getAll().map { s ->
            listOf(s.id, s.companyName, s.phone ?: "", s.email ?: "", s.address ?: "", s.deliveryDays ?: "", s.paymentTerms ?: "")
        }
        return writeFile(
            "suppliers.csv",
            buildCsv(listOf("id", "company_name", "phone", "email", "address", "delivery_days", "payment_terms"), rows)
        )
    }

    private suspend fun exportSupplierPrices(): File {
        val products = productDao.getAll().associateBy { it.id }
        val suppliers = supplierDao.getAll().associateBy { it.id }
        val rows = mutableListOf<List<Any?>>()
        for (link in supplierProductDao.getAll()) {
            val history = supplierPriceDao.getHistory(link.id)
            for (price in history) {
                rows.add(
                    listOf(
                        products[link.productId]?.name ?: "",
                        suppliers[link.supplierId]?.companyName ?: "",
                        link.packageDescription,
                        price.priceMinor / 100.0,
                        dateFormat.format(Date(price.effectiveDate))
                    )
                )
            }
        }
        return writeFile(
            "supplier_prices.csv",
            buildCsv(listOf("product", "supplier", "package", "price", "effective_date"), rows)
        )
    }

    private suspend fun exportRecipes(): File {
        val products = productDao.getAll().associateBy { it.id }
        val rows = mutableListOf<List<Any?>>()
        for (recipe in recipeDao.getAll()) {
            for (item in recipeItemDao.getForRecipe(recipe.id)) {
                rows.add(
                    listOf(
                        recipe.name, recipe.sellPriceMinor / 100.0,
                        products[item.productId]?.name ?: "", item.quantity, products[item.productId]?.unit ?: ""
                    )
                )
            }
        }
        return writeFile(
            "recipes.csv",
            buildCsv(listOf("recipe", "sell_price", "ingredient", "quantity", "unit"), rows)
        )
    }

    private suspend fun exportPurchaseOrders(): File {
        val suppliers = supplierDao.getAll().associateBy { it.id }
        val products = productDao.getAll().associateBy { it.id }
        val rows = mutableListOf<List<Any?>>()
        for (order in purchaseOrderDao.getAll()) {
            for (item in purchaseOrderItemDao.getForOrder(order.id)) {
                rows.add(
                    listOf(
                        order.poNumber, suppliers[order.supplierId]?.companyName ?: "", order.status,
                        products[item.productId]?.name ?: "", item.quantity, item.unitCostMinor / 100.0, item.receivedQuantity
                    )
                )
            }
        }
        return writeFile(
            "purchase_orders.csv",
            buildCsv(listOf("po_number", "supplier", "status", "product", "quantity", "unit_cost", "received_quantity"), rows)
        )
    }

    private suspend fun exportStockMovements(): File {
        val products = productDao.getAll().associateBy { it.id }
        val rows = movementDao.getAll().map { m ->
            listOf(
                dateFormat.format(Date(m.createdAt)), products[m.productId]?.name ?: "",
                m.type, m.quantity, m.reason, m.note ?: "", m.totalValueMinor / 100.0
            )
        }
        return writeFile(
            "stock_movements.csv",
            buildCsv(listOf("date", "product", "type", "quantity", "reason", "note", "value"), rows)
        )
    }

    private suspend fun exportWaste(): File {
        val products = productDao.getAll().associateBy { it.id }
        val rows = wasteDao.getAll().map { w ->
            listOf(
                dateFormat.format(Date(w.recordedDate)), products[w.productId]?.name ?: "",
                w.quantity, w.reason, w.totalCostMinor / 100.0, w.note ?: ""
            )
        }
        return writeFile(
            "waste_records.csv",
            buildCsv(listOf("date", "product", "quantity", "reason", "cost", "note"), rows)
        )
    }
}
