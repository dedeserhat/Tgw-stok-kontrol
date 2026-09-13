package com.tgw.stock.data.repository

import androidx.room.withTransaction
import com.tgw.stock.data.local.AppDatabase
import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.PurchaseOrderDao
import com.tgw.stock.data.local.dao.PurchaseOrderItemDao
import com.tgw.stock.data.local.entities.PurchaseOrderEntity
import com.tgw.stock.data.local.entities.PurchaseOrderItemEntity
import com.tgw.stock.domain.PurchaseOrderStatus
import com.tgw.stock.domain.StockCalculations
import com.tgw.stock.domain.StockInReason
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface PurchaseRepository {
    /** Products below their minimum, grouped by their cheapest supplier. */
    suspend fun buildPurchaseList(): List<SupplierPurchaseGroup>
    suspend fun buildPurchaseListLines(): List<PurchaseListLine>

    fun observeOrders(): Flow<List<PurchaseOrderEntity>>
    fun observeOrder(id: Long): Flow<PurchaseOrderEntity?>
    suspend fun getOrder(id: Long): PurchaseOrderEntity?
    fun observeOrderItems(orderId: Long): Flow<List<PurchaseOrderItemEntity>>
    suspend fun getOrderItems(orderId: Long): List<PurchaseOrderItemEntity>

    suspend fun createOrder(supplierId: Long, items: List<Pair<Long, Double>>, priceLookup: suspend (Long) -> Long): Long
    suspend fun updateOrderStatus(orderId: Long, status: PurchaseOrderStatus)
    suspend fun receiveOrder(orderId: Long, receivedLines: Map<Long, Double>, expiryDates: Map<Long, Long?>)
    suspend fun deleteOrder(order: PurchaseOrderEntity)
}

class RoomPurchaseRepository(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val orderDao: PurchaseOrderDao,
    private val orderItemDao: PurchaseOrderItemDao,
    private val supplierPriceRepository: SupplierPriceRepository,
    private val stockRepository: StockRepository
) : PurchaseRepository {

    override suspend fun buildPurchaseListLines(): List<PurchaseListLine> {
        val products = productDao.getAll()
        val lines = mutableListOf<PurchaseListLine>()
        for (product in products) {
            if (product.currentStock >= product.minStock) continue
            val qty = StockCalculations.quantityToPurchase(product.currentStock, product.targetStock)
            if (qty <= 0.0) continue
            val bestOffer = supplierPriceRepository.getCheapestOffer(product.id)
            val estimatedCost = if (bestOffer != null) {
                StockCalculations.estimatedPurchaseCost(qty, bestOffer.pricePerBaseUnitMinor)
            } else {
                StockCalculations.estimatedPurchaseCost(qty, product.lastCostPerUnitMinor)
            }
            lines.add(
                PurchaseListLine(
                    productId = product.id,
                    productName = product.name,
                    unit = product.unit,
                    currentStock = product.currentStock,
                    targetStock = product.targetStock,
                    quantityToBuy = qty,
                    bestOffer = bestOffer,
                    estimatedCostMinor = estimatedCost.cents
                )
            )
        }
        return lines.sortedBy { it.productName }
    }

    override suspend fun buildPurchaseList(): List<SupplierPurchaseGroup> {
        val lines = buildPurchaseListLines()
        val withSupplier = lines.filter { it.bestOffer != null }
        val withoutSupplier = lines.filter { it.bestOffer == null }

        val groups = withSupplier.groupBy { it.bestOffer!!.supplierId to it.bestOffer.supplierName }
            .map { (key, groupLines) ->
                SupplierPurchaseGroup(
                    supplierId = key.first,
                    supplierName = key.second,
                    lines = groupLines,
                    totalCostMinor = groupLines.sumOf { it.estimatedCostMinor }
                )
            }
            .sortedBy { it.supplierName }

        return if (withoutSupplier.isEmpty()) {
            groups
        } else {
            groups + SupplierPurchaseGroup(
                supplierId = -1,
                supplierName = "No supplier set",
                lines = withoutSupplier,
                totalCostMinor = withoutSupplier.sumOf { it.estimatedCostMinor }
            )
        }
    }

    override fun observeOrders(): Flow<List<PurchaseOrderEntity>> = orderDao.observeAll()
    override fun observeOrder(id: Long): Flow<PurchaseOrderEntity?> = orderDao.observeById(id)
    override suspend fun getOrder(id: Long): PurchaseOrderEntity? = orderDao.getById(id)
    override fun observeOrderItems(orderId: Long): Flow<List<PurchaseOrderItemEntity>> = orderItemDao.observeForOrder(orderId)
    override suspend fun getOrderItems(orderId: Long): List<PurchaseOrderItemEntity> = orderItemDao.getForOrder(orderId)

    override suspend fun createOrder(
        supplierId: Long,
        items: List<Pair<Long, Double>>,
        priceLookup: suspend (Long) -> Long
    ): Long = db.withTransaction {
        val poNumber = generatePoNumber()
        val orderId = orderDao.insert(
            PurchaseOrderEntity(
                poNumber = poNumber,
                supplierId = supplierId,
                status = PurchaseOrderStatus.DRAFT.name
            )
        )
        val orderItems = items.map { (productId, quantity) ->
            PurchaseOrderItemEntity(
                purchaseOrderId = orderId,
                productId = productId,
                quantity = quantity,
                unitCostMinor = priceLookup(productId)
            )
        }
        orderItemDao.insertAll(orderItems)
        orderId
    }

    private suspend fun generatePoNumber(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val count = orderDao.countAll() + 1
        return "PO-$year-${count.toString().padStart(4, '0')}"
    }

    override suspend fun updateOrderStatus(orderId: Long, status: PurchaseOrderStatus) {
        val order = orderDao.getById(orderId) ?: return
        val now = System.currentTimeMillis()
        orderDao.update(
            order.copy(
                status = status.name,
                orderedDate = if (status == PurchaseOrderStatus.ORDERED && order.orderedDate == null) now else order.orderedDate,
                deliveredDate = if (status == PurchaseOrderStatus.DELIVERED) now else order.deliveredDate
            )
        )
    }

    /** Marks a PO Delivered/Partially Delivered and creates real stock-in batches +
     * movements for every line received, keyed by purchase_order_item id -> quantity received. */
    override suspend fun receiveOrder(orderId: Long, receivedLines: Map<Long, Double>, expiryDates: Map<Long, Long?>) {
        db.withTransaction {
            val order = orderDao.getById(orderId) ?: return@withTransaction
            val items = orderItemDao.getForOrder(orderId)
            var allFullyReceived = true
            var anyReceived = false

            for (item in items) {
                val receivedQty = receivedLines[item.id] ?: 0.0
                if (receivedQty <= 0.0) {
                    if (item.receivedQuantity < item.quantity) allFullyReceived = false
                    continue
                }
                anyReceived = true
                val newReceivedTotal = item.receivedQuantity + receivedQty
                orderItemDao.updateReceived(item.id, newReceivedTotal)
                if (newReceivedTotal < item.quantity) allFullyReceived = false

                stockRepository.stockIn(
                    productId = item.productId,
                    quantity = receivedQty,
                    unitCostMinor = item.unitCostMinor,
                    reason = StockInReason.SUPPLIER_DELIVERY,
                    note = "Received from PO ${order.poNumber}",
                    expiryDate = expiryDates[item.id],
                    supplierId = order.supplierId,
                    relatedPurchaseOrderId = orderId
                )
            }

            val newStatus: String = when {
                !anyReceived -> order.status
                allFullyReceived -> PurchaseOrderStatus.DELIVERED.name
                else -> PurchaseOrderStatus.PARTIALLY_DELIVERED.name
            }
            orderDao.update(
                order.copy(
                    status = newStatus,
                    deliveredDate = if (newStatus == PurchaseOrderStatus.DELIVERED.name) System.currentTimeMillis() else order.deliveredDate
                )
            )
        }
    }

    override suspend fun deleteOrder(order: PurchaseOrderEntity) = orderDao.delete(order)
}
