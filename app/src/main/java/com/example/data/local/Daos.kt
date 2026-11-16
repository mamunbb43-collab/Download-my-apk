package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY currentDue DESC, name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAll(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY currentPayable DESC, name ASC")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers")
    suspend fun getAll(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Query("DELETE FROM suppliers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM suppliers")
    suspend fun clearAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getAll(): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: String): SaleEntity?

    @Query("SELECT * FROM sale_items ORDER BY id ASC")
    fun observeAllItems(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllItems(): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsForSale(saleId: String)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSaleById(id: String)

    @Query("DELETE FROM sale_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    suspend fun getAll(): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getById(id: String): PurchaseEntity?

    @Query("SELECT * FROM purchase_items")
    fun observeAllItems(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllItems(): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItemsForPurchase(purchaseId: String): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchases(purchases: List<PurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteItemsForPurchase(purchaseId: String)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: String)

    @Query("DELETE FROM purchase_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM purchases")
    suspend fun clearAllPurchases()
}

@Dao
interface DueTransactionDao {
    @Query("SELECT * FROM due_transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DueTransactionEntity>>

    @Query("SELECT * FROM due_transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<DueTransactionEntity>

    @Query("SELECT * FROM due_transactions WHERE id = :id")
    suspend fun getById(id: String): DueTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: DueTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<DueTransactionEntity>)

    @Query("DELETE FROM due_transactions WHERE referenceSaleId = :saleId")
    suspend fun deleteBySaleId(saleId: String)

    @Query("DELETE FROM due_transactions WHERE referencePurchaseId = :purchaseId")
    suspend fun deleteByPurchaseId(purchaseId: String)

    @Query("DELETE FROM due_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM due_transactions")
    suspend fun clearAll()
}

@Dao
interface CashTransactionDao {
    @Query("SELECT * FROM cash_transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CashTransactionEntity>>

    @Query("SELECT * FROM cash_transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<CashTransactionEntity>

    @Query("SELECT * FROM cash_transactions WHERE id = :id")
    suspend fun getById(id: String): CashTransactionEntity?

    @Query("SELECT * FROM cash_transactions WHERE referenceId = :referenceId LIMIT 1")
    suspend fun getByReferenceId(referenceId: String): CashTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: CashTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<CashTransactionEntity>)

    @Query("DELETE FROM cash_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cash_transactions WHERE referenceId = :referenceId")
    suspend fun deleteByReferenceId(referenceId: String)

    @Query("DELETE FROM cash_transactions")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    suspend fun getAll(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements WHERE referenceId = :referenceId")
    suspend fun deleteByReferenceId(referenceId: String)

    @Query("DELETE FROM stock_movements")
    suspend fun clearAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}
