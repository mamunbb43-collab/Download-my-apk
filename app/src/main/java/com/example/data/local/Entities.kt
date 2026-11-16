package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val code: String = "",
    val barcode: String = "",
    val category: String = "সাধারণ",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStock: Double = 5.0,
    val unit: String = "পিস",
    val description: String = "",
    val imageUri: String = "",
    val totalPurchasedQty: Double = 0.0,
    val totalSoldQty: Double = 0.0,
    val totalDamagedQty: Double = 0.0,
    val totalReturnedQty: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalPurchaseAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val currentDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val totalPurchaseAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val currentPayable: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String = "",
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val totalCostOfGoods: Double,
    val paymentMethod: String, // CASH, BANK, MOBILE_WALLET, DUE
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReturned: Boolean = false
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val costPrice: Double,
    val lineTotal: Double
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val supplierId: String? = null,
    val supplierName: String,
    val supplierPhone: String = "",
    val productSubtotal: Double,
    val transportCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentMethod: String, // CASH, BANK, MOBILE_WALLET, DUE
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isReturned: Boolean = false
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitCost: Double,
    val lineTotal: Double
)

@Entity(tableName = "due_transactions")
data class DueTransactionEntity(
    @PrimaryKey val id: String,
    val partyType: String, // CUSTOMER or SUPPLIER
    val partyId: String,
    val partyName: String,
    val partyPhone: String = "",
    val txType: String, // DUE_ADDED, COLLECTION, PAYMENT
    val amount: Double,
    val paymentMethod: String = "CASH",
    val note: String = "",
    val referenceSaleId: String? = null,
    val referencePurchaseId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_transactions")
data class CashTransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // IN or OUT
    val category: String, // SALE_INCOME, DUE_COLLECTION, PURCHASE_PAYMENT, EXPENSE, SUPPLIER_PAYMENT, OTHER_INCOME, OTHER_EXPENSE, CAPITAL_INVESTMENT, SALE_RETURN_REFUND, PURCHASE_RETURN_REFUND
    val accountType: String, // CASH, BANK, MOBILE_WALLET
    val amount: Double,
    val description: String,
    val referenceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // RENT, ELECTRICITY, SALARY, TRANSPORT, FOOD, OTHER
    val amount: Double,
    val paymentMethod: String = "CASH",
    val note: String = "",
    val receiptUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val movementType: String, // PURCHASE, SALE, DAMAGE, SALE_RETURN, PURCHASE_RETURN, ADJUSTMENT
    val quantityDelta: Double,
    val stockAfter: Double,
    val note: String = "",
    val referenceId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE, RETURN, BACKUP, RESTORE, SECURITY
    val summary: String,
    val amountImpact: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
