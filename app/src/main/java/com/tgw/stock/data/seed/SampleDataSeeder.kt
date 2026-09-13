package com.tgw.stock.data.seed

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.PurchaseOrderDao
import com.tgw.stock.data.local.dao.PurchaseOrderItemDao
import com.tgw.stock.data.local.entities.*
import com.tgw.stock.data.repository.*
import com.tgw.stock.domain.PurchaseOrderStatus
import com.tgw.stock.domain.StockInReason
import com.tgw.stock.domain.StockOutReason
import com.tgw.stock.domain.StockUnit
import com.tgw.stock.domain.WasteReason
import java.util.concurrent.TimeUnit

/**
 * Fills a brand-new database with realistic restaurant data so every screen has
 * something to show on first launch. Everything it creates is flagged isSample = true
 * (propagated through StockRepository/WasteRepository too) so Settings > Clear Sample
 * Data can wipe it cleanly without touching anything the user has since entered.
 */
class SampleDataSeeder(
    private val productDao: ProductDao,
    private val categoryRepository: CategoryRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierPriceRepository: SupplierPriceRepository,
    private val recipeRepository: RecipeRepository,
    private val stockRepository: StockRepository,
    private val wasteRepository: WasteRepository,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val purchaseOrderItemDao: PurchaseOrderItemDao
) {
    private val now = System.currentTimeMillis()
    private fun daysAgo(d: Long) = now - TimeUnit.DAYS.toMillis(d)
    private fun daysFromNow(d: Long) = now + TimeUnit.DAYS.toMillis(d)

    suspend fun seedIfEmpty() {
        if (productDao.getAll().isNotEmpty()) return

        val categoryIds = seedCategories()
        val supplierIds = seedSuppliers()
        val productIds = seedProducts(categoryIds)
        seedSupplierPrices(supplierIds, productIds)
        seedInitialStock(supplierIds, productIds)
        seedRecipes(productIds)
        seedSampleMovementsAndWaste(productIds)
        seedSamplePurchaseOrder(supplierIds, productIds)
    }

    private suspend fun seedCategories(): Map<String, Long> {
        val names = listOf("Meat", "Dairy", "Vegetables", "Frozen", "Dry Food", "Sauces", "Drinks", "Packaging", "Cleaning", "Other")
        return names.mapIndexed { index, name ->
            name to categoryRepository.upsert(CategoryEntity(name = name, sortOrder = index, isSample = true))
        }.toMap()
    }

    private suspend fun seedSuppliers(): Map<String, Long> {
        val suppliers = listOf(
            SupplierEntity(companyName = "Istanbul Meats", phone = "+353 1 555 0101", email = "orders@istanbulmeats.ie", address = "Unit 4, Food Market, Dublin", deliveryDays = "Mon,Wed,Fri", paymentTerms = "Net 14", isSample = true),
            SupplierEntity(companyName = "Musgrave", phone = "+353 1 555 0102", email = "sales@musgrave.ie", address = "Musgrave Distribution Centre, Cork", deliveryDays = "Tue,Thu", paymentTerms = "Net 30", isSample = true),
            SupplierEntity(companyName = "Sysco", phone = "+353 1 555 0103", email = "orders@sysco.ie", address = "Sysco Ireland, Dublin", deliveryDays = "Mon,Tue,Wed,Thu,Fri", paymentTerms = "Net 30", isSample = true),
            SupplierEntity(companyName = "Balkania", phone = "+353 1 555 0104", email = "info@balkania.ie", address = "Balkania Foods, Dublin", deliveryDays = "Wed,Fri", paymentTerms = "Net 14", isSample = true),
            SupplierEntity(companyName = "Robinsons", phone = "+353 1 555 0105", email = "sales@robinsons.ie", address = "Robinsons Foodservice, Dublin", deliveryDays = "Mon,Thu", paymentTerms = "COD", isSample = true),
            SupplierEntity(companyName = "Frylite", phone = "+353 1 555 0106", email = "orders@frylite.com", address = "Frylite, Newry", deliveryDays = "Tue", paymentTerms = "Net 30", minOrderMinor = 5000, isSample = true)
        )
        return suppliers.associate { it.companyName to supplierRepository.upsert(it) }
    }

    private suspend fun seedProducts(cat: Map<String, Long>): Map<String, Long> {
        data class P(val name: String, val category: String, val unit: StockUnit, val current: Double, val min: Double, val target: Double, val barcode: String? = null)

        val defs = listOf(
            P("Yoğurt", "Dairy", StockUnit.KG, 3.0, 5.0, 15.0, "8690000000011"),
            P("Domates", "Vegetables", StockUnit.KG, 4.0, 6.0, 15.0, "8690000000028"),
            P("Tavuk", "Meat", StockUnit.KG, 12.0, 10.0, 25.0, "8690000000035"),
            P("Kuzu eti", "Meat", StockUnit.KG, 6.0, 8.0, 18.0),
            P("Dana eti", "Meat", StockUnit.KG, 9.0, 8.0, 20.0),
            P("Mozzarella", "Dairy", StockUnit.KG, 7.0, 6.0, 15.0),
            P("Gouda", "Dairy", StockUnit.KG, 4.0, 4.0, 10.0),
            P("Un", "Dry Food", StockUnit.KG, 20.0, 15.0, 40.0),
            P("Patates", "Vegetables", StockUnit.KG, 18.0, 10.0, 25.0),
            P("Soğan", "Vegetables", StockUnit.KG, 10.0, 6.0, 15.0),
            P("Mayonez", "Sauces", StockUnit.KG, 5.0, 4.0, 12.0),
            P("Sarımsak", "Vegetables", StockUnit.KG, 2.0, 2.0, 5.0),
            P("Chilli sauce", "Sauces", StockUnit.L, 3.0, 3.0, 8.0),
            P("Pizza sauce", "Sauces", StockUnit.L, 6.0, 5.0, 12.0),
            P("Salatalık", "Vegetables", StockUnit.KG, 3.0, 4.0, 10.0),
            P("Tuz", "Dry Food", StockUnit.KG, 8.0, 3.0, 10.0),
            P("Mint", "Dry Food", StockUnit.G, 200.0, 100.0, 400.0),
            P("Ayçiçek Yağı", "Dry Food", StockUnit.L, 15.0, 10.0, 30.0)
        )

        return defs.associate { d ->
            d.name to productDao.insert(
                ProductEntity(
                    name = d.name,
                    categoryId = cat[d.category],
                    unit = d.unit.name,
                    currentStock = 0.0, // stock is added afterwards via stockRepository so batches/movements exist
                    minStock = d.min,
                    targetStock = d.target,
                    barcode = d.barcode,
                    isSample = true
                )
            )
        }
    }

    private suspend fun seedSupplierPrices(sup: Map<String, Long>, prod: Map<String, Long>) {
        suspend fun offer(supplier: String, product: String, packageDesc: String, packageQty: Double, priceHistory: List<Pair<Long, Long>>) {
            val supplierId = sup[supplier] ?: return
            val productId = prod[product] ?: return
            var linkId: Long? = null
            for ((priceMinor, daysBack) in priceHistory) {
                linkId = supplierPriceRepository.addOrUpdateOffer(
                    supplierId, productId, packageDesc, packageQty, priceMinor, daysAgo(daysBack), linkId
                )
            }
        }

        // Yoğurt: two competing suppliers, Musgrave has a rising price history (spec example).
        offer("Musgrave", "Yoğurt", "10 kg tub", 10.0, listOf(1750L to 75L, 1800L to 29L, 1890L to 3L))
        offer("Sysco", "Yoğurt", "10 kg tub", 10.0, listOf(2150L to 10L))

        offer("Istanbul Meats", "Tavuk", "10 kg box", 10.0, listOf(5400L to 20L, 5600L to 2L))
        offer("Istanbul Meats", "Kuzu eti", "5 kg box", 5.0, listOf(6500L to 15L))
        offer("Istanbul Meats", "Dana eti", "5 kg box", 5.0, listOf(5800L to 15L))

        offer("Sysco", "Domates", "10 kg box", 10.0, listOf(1400L to 12L, 1550L to 1L))
        offer("Sysco", "Patates", "25 kg bag", 25.0, listOf(1200L to 12L))
        offer("Sysco", "Soğan", "20 kg bag", 20.0, listOf(1000L to 12L))
        offer("Sysco", "Sarımsak", "2 kg bag", 2.0, listOf(900L to 12L))
        offer("Sysco", "Salatalık", "10 kg box", 10.0, listOf(1300L to 8L))

        offer("Musgrave", "Mozzarella", "5 kg block", 5.0, listOf(3200L to 20L))
        offer("Musgrave", "Gouda", "3 kg block", 3.0, listOf(2100L to 20L))
        offer("Musgrave", "Un", "25 kg bag", 25.0, listOf(1800L to 25L))
        offer("Musgrave", "Mayonez", "10 kg bucket", 10.0, listOf(2200L to 25L))
        offer("Robinsons", "Un", "25 kg bag", 25.0, listOf(1950L to 5L))

        offer("Balkania", "Chilli sauce", "5 L bottle", 5.0, listOf(1500L to 18L))
        offer("Balkania", "Pizza sauce", "10 L box", 10.0, listOf(2400L to 18L))
        offer("Balkania", "Tuz", "25 kg bag", 25.0, listOf(800L to 40L))
        offer("Balkania", "Mint", "500 g bag", 500.0, listOf(400L to 10L))

        offer("Frylite", "Ayçiçek Yağı", "20 L drum", 20.0, listOf(3600L to 22L))
    }

    private suspend fun seedInitialStock(sup: Map<String, Long>, prod: Map<String, Long>) {
        suspend fun receive(product: String, quantity: Double, unitCostMinor: Long, supplier: String, expiryDays: Long?) {
            val productId = prod[product] ?: return
            stockRepository.stockIn(
                productId = productId,
                quantity = quantity,
                unitCostMinor = unitCostMinor,
                reason = StockInReason.SUPPLIER_DELIVERY,
                note = "Initial stock",
                expiryDate = expiryDays?.let { daysFromNow(it) },
                supplierId = sup[supplier],
                isSample = true
            )
        }

        receive("Yoğurt", 3.0, 189, "Musgrave", 10)
        receive("Domates", 4.0, 155, "Sysco", 6)
        receive("Tavuk", 12.0, 560, "Istanbul Meats", 4)
        receive("Kuzu eti", 6.0, 1300, "Istanbul Meats", 5)
        receive("Dana eti", 9.0, 1160, "Istanbul Meats", 5)
        receive("Mozzarella", 7.0, 640, "Musgrave", 14)
        receive("Gouda", 4.0, 700, "Musgrave", 30)
        receive("Un", 20.0, 72, "Musgrave", 120)
        receive("Patates", 18.0, 48, "Sysco", 30)
        receive("Soğan", 10.0, 50, "Sysco", 30)
        receive("Mayonez", 5.0, 220, "Musgrave", 60)
        receive("Sarımsak", 2.0, 450, "Sysco", 21)
        receive("Chilli sauce", 3.0, 300, "Balkania", 90)
        receive("Pizza sauce", 6.0, 240, "Balkania", 90)
        receive("Salatalık", 3.0, 130, "Sysco", 7)
        receive("Tuz", 8.0, 32, "Balkania", 365)
        receive("Mint", 200.0, 1, "Balkania", 14)
        receive("Ayçiçek Yağı", 15.0, 180, "Frylite", 180)
    }

    private suspend fun seedRecipes(prod: Map<String, Long>) {
        suspend fun recipe(name: String, sellPriceMinor: Long, items: List<Pair<String, Double>>) {
            val recipeId = recipeRepository.upsert(RecipeEntity(name = name, sellPriceMinor = sellPriceMinor, isSample = true))
            for ((productName, qty) in items) {
                val productId = prod[productName] ?: continue
                recipeRepository.upsertItem(RecipeItemEntity(recipeId = recipeId, productId = productId, quantity = qty, isSample = true))
            }
        }

        // RecipeItemEntity.quantity is always in the ingredient's own base unit (see
        // seedProducts above): kg for everything here except Mint (g) and the two sauces
        // (L). A "150 g of yoğurt" per-portion amount is therefore 0.15, not 150 - entering
        // it as 150 would mean 150 kg of yoğurt in one portion.
        recipe("Cacık", 499, listOf("Yoğurt" to 0.150, "Salatalık" to 0.040, "Sarımsak" to 0.005, "Tuz" to 0.002, "Mint" to 1.0))
        recipe("TGW Sauce", 250, listOf("Yoğurt" to 0.040, "Mayonez" to 0.060, "Sarımsak" to 0.010, "Chilli sauce" to 0.020))
        recipe("Garlic Sauce", 250, listOf("Yoğurt" to 0.050, "Mayonez" to 0.100, "Sarımsak" to 0.015))
        recipe("Chicken Shish", 999, listOf("Tavuk" to 0.220, "Soğan" to 0.030, "Domates" to 0.030))
        recipe("Iskender", 1250, listOf("Dana eti" to 0.200, "Pizza sauce" to 0.050, "Yoğurt" to 0.100, "Un" to 0.100))
        recipe("Kebab Bowl", 1099, listOf("Kuzu eti" to 0.180, "Patates" to 0.100, "Domates" to 0.050, "Soğan" to 0.030))
        recipe("Chicken Wrap", 799, listOf("Tavuk" to 0.150, "Domates" to 0.030, "Soğan" to 0.020, "Mayonez" to 0.020))
        recipe("Margherita Pizza", 899, listOf("Un" to 0.200, "Pizza sauce" to 0.080, "Mozzarella" to 0.150))
        recipe("A La Turca Pizza", 1099, listOf("Un" to 0.200, "Pizza sauce" to 0.060, "Mozzarella" to 0.100, "Dana eti" to 0.080, "Soğan" to 0.020))
    }

    private suspend fun seedSampleMovementsAndWaste(prod: Map<String, Long>) {
        prod["Tavuk"]?.let { stockRepository.stockOut(it, 2.0, StockOutReason.KITCHEN_USAGE, "Prep for service", isSample = true) }
        prod["Domates"]?.let { stockRepository.stockOut(it, 1.5, StockOutReason.KITCHEN_USAGE, "Prep for service", isSample = true) }
        prod["Tavuk"]?.let { wasteRepository.recordWaste(it, 0.5, WasteReason.EXPIRED, "Past use-by date", isSample = true) }
        prod["Mozzarella"]?.let { wasteRepository.recordWaste(it, 0.3, WasteReason.DAMAGE, "Dropped tray", isSample = true) }
    }

    private suspend fun seedSamplePurchaseOrder(sup: Map<String, Long>, prod: Map<String, Long>) {
        val supplierId = sup["Istanbul Meats"] ?: return
        val orderId = purchaseOrderDao.insert(
            PurchaseOrderEntity(
                poNumber = "PO-DEMO-0001",
                supplierId = supplierId,
                status = PurchaseOrderStatus.DRAFT.name,
                notes = "Sample draft order - safe to delete",
                isSample = true
            )
        )
        val chickenId = prod["Tavuk"] ?: return
        purchaseOrderItemDao.insert(
            PurchaseOrderItemEntity(
                purchaseOrderId = orderId,
                productId = chickenId,
                quantity = 20.0,
                unitCostMinor = 560,
                isSample = true
            )
        )
    }
}
