package com.tgw.stock.data.local.dao

import androidx.room.*
import com.tgw.stock.data.local.entities.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY companyName")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY companyName")
    suspend fun getAll(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    fun observeById(id: Long): Flow<SupplierEntity?>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity): Long

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Delete
    suspend fun delete(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers WHERE isSample = 1")
    suspend fun deleteSample()
}
