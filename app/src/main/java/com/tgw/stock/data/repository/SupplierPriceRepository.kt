package com.tgw.stock.data.repository

import com.tgw.stock.data.local.dao.ProductDao
import com.tgw.stock.data.local.dao.SupplierDao
import com.tgw.stock.data.local.dao.SupplierPriceDao
import com.tgw.stock.data.local.dao.SupplierProductDao
import com.tgw.stock.data.local.entities.SupplierPriceEntity
import com.tgw.stock.data.local.entities.SupplierProductEntity
import com.tgw.stock.domain.StockCalculations
import kotlinx.coroutines.flow.Flow

interface SupplierPriceRepository {
    fun observeLinksForProduct(productId: Long): Flow<List<SupplierProductEntity>>
    suspend fun getOffersForProduct(productId: Long): List<SupplierOffer>
    suspend fun getCheapestOffer(productId: Long): SupplierOffer?
    fun observePriceHistory(supplierProductId: Long): Flow<List<SupplierPriceEntity>>
    suspend fun getSupplierProduct(id: Long): SupplierProductEntity?
    suspend fun addOrUpdateOffer(
        supplierId: Long,
        productId: Long,
        packageDescription: String,
        packageQuantity: Double,
        priceMinor: Long,
        effectiveDate: Long,
        existingLinkId: Long? = null
    ): Long
    suspend fun deleteLink(link: SupplierProductEntity)
    suspend fun getAllOffersGroupedByProduct(): Map<Long, List<SupplierOffer>>
    suspend fun getOffersForSupplier(supplierId: Long): List<Pair<String, SupplierOffer>>
}

class RoomSupplierPriceRepository(
    private val supplierProductDao: SupplierProductDao,
    private val supplierPriceDao: SupplierPriceDao,
    private val supplierDao: SupplierDao,
    private val productDao: ProductDao
) : SupplierPriceRepository {

    override fun observeLinksForProduct(productId: Long): Flow<List<SupplierProductEntity>> =
        supplierProductDao.observeForProduct(productId)

    override suspend fun getOffersForProduct(productId: Long): List<SupplierOffer> {
        val links = supplierProductDao.getForProduct(productId)
        return links.mapNotNull { buildOffer(it) }
    }

    private suspend fun buildOffer(link: SupplierProductEntity): SupplierOffer? {
        val latest = supplierPriceDao.getLatest(link.id) ?: return null
        val supplier = supplierDao.getById(link.supplierId) ?: return null
        return SupplierOffer(
            supplierProductId = link.id,
            supplierId = link.supplierId,
            supplierName = supplier.companyName,
            packageDescription = link.packageDescription,
            packageQuantity = link.packageQuantity,
            latestPriceMinor = latest.priceMinor,
            pricePerBaseUnitMinor = StockCalculations.pricePerBaseUnit(latest.priceMinor, link.packageQuantity),
            priceDate = latest.effectiveDate
        )
    }

    override suspend fun getCheapestOffer(productId: Long): SupplierOffer? =
        getOffersForProduct(productId).minByOrNull { it.pricePerBaseUnitMinor }

    override fun observePriceHistory(supplierProductId: Long): Flow<List<SupplierPriceEntity>> =
        supplierPriceDao.observeHistory(supplierProductId)

    override suspend fun getSupplierProduct(id: Long): SupplierProductEntity? =
        supplierProductDao.getById(id)

    override suspend fun addOrUpdateOffer(
        supplierId: Long,
        productId: Long,
        packageDescription: String,
        packageQuantity: Double,
        priceMinor: Long,
        effectiveDate: Long,
        existingLinkId: Long?
    ): Long {
        val linkId = if (existingLinkId != null) {
            supplierProductDao.update(
                SupplierProductEntity(
                    id = existingLinkId,
                    supplierId = supplierId,
                    productId = productId,
                    packageDescription = packageDescription,
                    packageQuantity = packageQuantity
                )
            )
            existingLinkId
        } else {
            supplierProductDao.insert(
                SupplierProductEntity(
                    supplierId = supplierId,
                    productId = productId,
                    packageDescription = packageDescription,
                    packageQuantity = packageQuantity
                )
            )
        }
        supplierPriceDao.insert(
            SupplierPriceEntity(
                supplierProductId = linkId,
                priceMinor = priceMinor,
                effectiveDate = effectiveDate
            )
        )
        return linkId
    }

    override suspend fun deleteLink(link: SupplierProductEntity) = supplierProductDao.delete(link)

    override suspend fun getAllOffersGroupedByProduct(): Map<Long, List<SupplierOffer>> {
        val allLinks = supplierProductDao.getAll()
        return allLinks.groupBy { it.productId }.mapValues { (_, links) ->
            links.mapNotNull { buildOffer(it) }
        }
    }

    override suspend fun getOffersForSupplier(supplierId: Long): List<Pair<String, SupplierOffer>> {
        val links = supplierProductDao.getAll().filter { it.supplierId == supplierId }
        return links.mapNotNull { link ->
            val offer = buildOffer(link) ?: return@mapNotNull null
            val productName = productDao.getById(link.productId)?.name ?: return@mapNotNull null
            productName to offer
        }
    }
}
