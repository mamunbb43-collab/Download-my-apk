package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.ShopSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

data class CartLineInput(
    val product: ProductEntity,
    val quantity: Double,
    val unitPrice: Double
)

data class PurchaseLineInput(
    val product: ProductEntity,
    val quantity: Double,
    val unitCost: Double
)

data class DashboardMetrics(
    val todaySalesCount: Int = 0,
    val todayTotalSales: Double = 0.0,
    val todayTotalPurchase: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val todayTotalExpense: Double = 0.0,
    val totalCash: Double = 0.0,
    val totalBankAndWallet: Double = 0.0,
    val totalCustomerReceivable: Double = 0.0,
    val totalSupplierPayable: Double = 0.0,
    val totalProductCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val totalPotentialSaleValue: Double = 0.0
)

data class FinancialOverview(
    val inventoryCostValue: Double = 0.0,
    val cashInHand: Double = 0.0,
    val bankAndWalletBalance: Double = 0.0,
    val customerReceivables: Double = 0.0,
    val supplierPayables: Double = 0.0,
    val totalCapitalInvestment: Double = 0.0,
    val netBusinessAssets: Double = 0.0,
    val allTimeNetProfit: Double = 0.0
)

data class ProfitLossSummary(
    val totalSales: Double = 0.0,
    val totalCogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val otherIncome: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val netProfit: Double = 0.0
)

class BusinessRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val customers: Flow<List<CustomerEntity>> = db.customerDao().observeAll()
    val suppliers: Flow<List<SupplierEntity>> = db.supplierDao().observeAll()
    val sales: Flow<List<SaleEntity>> = db.saleDao().observeAll()
    val saleItems: Flow<List<SaleItemEntity>> = db.saleDao().observeAllItems()
    val purchases: Flow<List<PurchaseEntity>> = db.purchaseDao().observeAll()
    val purchaseItems: Flow<List<PurchaseItemEntity>> = db.purchaseDao().observeAllItems()
    val dueTransactions: Flow<List<DueTransactionEntity>> = db.dueTransactionDao().observeAll()
    val cashTransactions: Flow<List<CashTransactionEntity>> = db.cashTransactionDao().observeAll()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    val stockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().observeAll()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().observeRecent()

    fun observeDashboardMetrics(): Flow<DashboardMetrics> {
        return combine(
            products,
            customers,
            suppliers,
            sales,
            purchases,
            cashTransactions,
            expenses,
            settingsRepository.settingsFlow
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val prods = values[0] as List<ProductEntity>
            @Suppress("UNCHECKED_CAST")
            val custs = values[1] as List<CustomerEntity>
            @Suppress("UNCHECKED_CAST")
            val supps = values[2] as List<SupplierEntity>
            @Suppress("UNCHECKED_CAST")
            val saleList = values[3] as List<SaleEntity>
            @Suppress("UNCHECKED_CAST")
            val purchaseList = values[4] as List<PurchaseEntity>
            @Suppress("UNCHECKED_CAST")
            val cashList = values[5] as List<CashTransactionEntity>
            @Suppress("UNCHECKED_CAST")
            val expList = values[6] as List<ExpenseEntity>
            val settings = values[7] as ShopSettings

            val (dayStart, dayEnd) = dayRangeMillis(System.currentTimeMillis())
            val todaySales = saleList.filter { !it.isReturned && it.timestamp in dayStart..dayEnd }
            val todayPurchases = purchaseList.filter { !it.isReturned && it.timestamp in dayStart..dayEnd }
            val todayExpenses = expList.filter { it.timestamp in dayStart..dayEnd }
            val todayOtherInc = cashList.filter {
                it.timestamp in dayStart..dayEnd && it.type == "IN" && it.category == "OTHER_INCOME"
            }.sumOf { it.amount }
            val todayOtherExp = cashList.filter {
                it.timestamp in dayStart..dayEnd && it.type == "OUT" && it.category == "OTHER_EXPENSE"
            }.sumOf { it.amount }

            val todaySalesSum = todaySales.sumOf { it.totalAmount }
            val todayCogs = todaySales.sumOf { it.totalCostOfGoods }
            val todayExpSum = todayExpenses.sumOf { it.amount } + todayOtherExp
            val todayProfit = (todaySalesSum - todayCogs) + todayOtherInc - todayExpSum

            val cashBalance = computeAccountBalance(settings.openingCashBalance, cashList, "CASH")
            val bankBalance = computeAccountBalance(settings.openingBankBalance, cashList, "BANK") +
                computeAccountBalance(settings.openingWalletBalance, cashList, "MOBILE_WALLET")

            DashboardMetrics(
                todaySalesCount = todaySales.size,
                todayTotalSales = todaySalesSum,
                todayTotalPurchase = todayPurchases.sumOf { it.totalAmount },
                todayNetProfit = todayProfit,
                todayTotalExpense = todayExpSum,
                totalCash = cashBalance,
                totalBankAndWallet = bankBalance,
                totalCustomerReceivable = custs.sumOf { it.currentDue },
                totalSupplierPayable = supps.sumOf { it.currentPayable },
                totalProductCount = prods.size,
                lowStockCount = prods.count { it.currentStock <= it.minStock },
                totalInventoryValue = prods.sumOf { (it.currentStock * it.purchasePrice).coerceAtLeast(0.0) },
                totalPotentialSaleValue = prods.sumOf { (it.currentStock * it.sellingPrice).coerceAtLeast(0.0) }
            )
        }
    }

    fun observeFinancialOverview(): Flow<FinancialOverview> {
        return combine(
            products,
            customers,
            suppliers,
            sales,
            cashTransactions,
            expenses,
            settingsRepository.settingsFlow
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val prods = values[0] as List<ProductEntity>
            @Suppress("UNCHECKED_CAST")
            val custs = values[1] as List<CustomerEntity>
            @Suppress("UNCHECKED_CAST")
            val supps = values[2] as List<SupplierEntity>
            @Suppress("UNCHECKED_CAST")
            val saleList = values[3] as List<SaleEntity>
            @Suppress("UNCHECKED_CAST")
            val cashList = values[4] as List<CashTransactionEntity>
            @Suppress("UNCHECKED_CAST")
            val expList = values[5] as List<ExpenseEntity>
            val settings = values[6] as ShopSettings

            val invValue = prods.sumOf { (it.currentStock * it.purchasePrice).coerceAtLeast(0.0) }
            val cashInHand = computeAccountBalance(settings.openingCashBalance, cashList, "CASH")
            val bankWallet = computeAccountBalance(settings.openingBankBalance, cashList, "BANK") +
                computeAccountBalance(settings.openingWalletBalance, cashList, "MOBILE_WALLET")
            val receivable = custs.sumOf { it.currentDue }
            val payable = supps.sumOf { it.currentPayable }
            val capitalAdded = cashList.filter { it.type == "IN" && it.category == "CAPITAL_INVESTMENT" }.sumOf { it.amount }
            val totalInvested = settings.openingCashBalance + settings.openingBankBalance + settings.openingWalletBalance + capitalAdded
            val netAssets = invValue + cashInHand + bankWallet + receivable - payable

            val pnl = computeProfitLoss(saleList, cashList, expList, 0L, Long.MAX_VALUE)

            FinancialOverview(
                inventoryCostValue = invValue,
                cashInHand = cashInHand,
                bankAndWalletBalance = bankWallet,
                customerReceivables = receivable,
                supplierPayables = payable,
                totalCapitalInvestment = totalInvested,
                netBusinessAssets = netAssets,
                allTimeNetProfit = pnl.netProfit
            )
        }
    }

    fun computeProfitLoss(
        salesList: List<SaleEntity>,
        cashList: List<CashTransactionEntity>,
        expensesList: List<ExpenseEntity>,
        startMillis: Long,
        endMillis: Long
    ): ProfitLossSummary {
        val validSales = salesList.filter { !it.isReturned && it.timestamp in startMillis..endMillis }
        val totalSales = validSales.sumOf { it.totalAmount }
        val totalCogs = validSales.sumOf { it.totalCostOfGoods }
        val grossProfit = totalSales - totalCogs
        val otherIncome = cashList.filter {
            it.timestamp in startMillis..endMillis && it.type == "IN" && it.category == "OTHER_INCOME"
        }.sumOf { it.amount }
        val otherExpenses = cashList.filter {
            it.timestamp in startMillis..endMillis && it.type == "OUT" && it.category == "OTHER_EXPENSE"
        }.sumOf { it.amount }
        val operatingExpenses = expensesList.filter { it.timestamp in startMillis..endMillis }.sumOf { it.amount } + otherExpenses
        val netProfit = grossProfit + otherIncome - operatingExpenses
        return ProfitLossSummary(
            totalSales = totalSales,
            totalCogs = totalCogs,
            grossProfit = grossProfit,
            otherIncome = otherIncome,
            operatingExpenses = operatingExpenses,
            netProfit = netProfit
        )
    }

    private fun computeAccountBalance(
        opening: Double,
        txs: List<CashTransactionEntity>,
        accountType: String
    ): Double {
        var balance = opening
        for (tx in txs) {
            if (tx.accountType == accountType) {
                if (tx.type == "IN") balance += tx.amount else balance -= tx.amount
            }
        }
        return balance
    }

    // --- PRODUCTS & STOCK ---
    suspend fun upsertProduct(product: ProductEntity, isNew: Boolean) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val finalProduct = if (isNew) {
                product.copy(
                    id = product.id.ifBlank { UUID.randomUUID().toString() },
                    totalPurchasedQty = product.currentStock.coerceAtLeast(0.0),
                    createdAt = now,
                    updatedAt = now
                )
            } else {
                product.copy(updatedAt = now)
            }
            db.productDao().upsert(finalProduct)
            if (isNew && finalProduct.currentStock > 0) {
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = finalProduct.id,
                        productName = finalProduct.name,
                        movementType = "ADJUSTMENT",
                        quantityDelta = finalProduct.currentStock,
                        stockAfter = finalProduct.currentStock,
                        note = "Initial stock",
                        timestamp = now
                    )
                )
            }
            logAudit(
                entityType = "PRODUCT",
                entityId = finalProduct.id,
                action = if (isNew) "CREATE" else "UPDATE",
                summary = "${finalProduct.name} (Stock: ${finalProduct.currentStock})",
                amountImpact = finalProduct.sellingPrice
            )
        }
        maybeAutoBackup()
    }

    suspend fun deleteProduct(product: ProductEntity) {
        db.withTransaction {
            db.productDao().deleteById(product.id)
            logAudit("PRODUCT", product.id, "DELETE", "Deleted product: ${product.name}", 0.0)
        }
        maybeAutoBackup()
    }

    suspend fun adjustStock(
        productId: String,
        movementType: String, // DAMAGE, SALE_RETURN, PURCHASE_RETURN, ADJUSTMENT
        quantityDelta: Double,
        note: String
    ) {
        db.withTransaction {
            val product = db.productDao().getById(productId) ?: return@withTransaction
            val newStock = (product.currentStock + quantityDelta).coerceAtLeast(0.0)
            val updated = when (movementType) {
                "DAMAGE" -> product.copy(
                    currentStock = newStock,
                    totalDamagedQty = product.totalDamagedQty + kotlin.math.abs(quantityDelta),
                    updatedAt = System.currentTimeMillis()
                )
                "SALE_RETURN" -> product.copy(
                    currentStock = newStock,
                    totalReturnedQty = product.totalReturnedQty + kotlin.math.abs(quantityDelta),
                    updatedAt = System.currentTimeMillis()
                )
                "PURCHASE_RETURN" -> product.copy(
                    currentStock = newStock,
                    totalReturnedQty = product.totalReturnedQty + kotlin.math.abs(quantityDelta),
                    updatedAt = System.currentTimeMillis()
                )
                else -> product.copy(
                    currentStock = newStock,
                    updatedAt = System.currentTimeMillis()
                )
            }
            db.productDao().upsert(updated)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    productId = product.id,
                    productName = product.name,
                    movementType = movementType,
                    quantityDelta = quantityDelta,
                    stockAfter = newStock,
                    note = note
                )
            )
            logAudit("STOCK", product.id, "UPDATE", "${product.name} ($movementType: $quantityDelta)", 0.0)
        }
        maybeAutoBackup()
    }

    // --- SALES ---
    suspend fun createSale(
        customerId: String?,
        customerName: String,
        customerPhone: String,
        lines: List<CartLineInput>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ): Pair<SaleEntity, List<ProductEntity>> {
        val lowStockAlerts = mutableListOf<ProductEntity>()
        val createdSale = db.withTransaction {
            val now = System.currentTimeMillis()
            val saleId = UUID.randomUUID().toString()
            val invoiceNumber = "INV-${(now % 1000000).toString().padStart(6, '0')}"

            val subtotal = lines.sumOf { it.unitPrice * it.quantity }
            val totalAmount = (subtotal - discount).coerceAtLeast(0.0)
            val effectivePaid = if (paymentMethod == "DUE") 0.0 else paidAmount.coerceIn(0.0, totalAmount)
            val dueAmount = (totalAmount - effectivePaid).coerceAtLeast(0.0)
            val totalCogs = lines.sumOf { it.product.purchasePrice * it.quantity }

            // Resolve or create customer if name provided with due
            var resolvedCustomerId = customerId
            if (resolvedCustomerId == null && dueAmount > 0 && customerName.isNotBlank()) {
                val newCust = CustomerEntity(
                    id = UUID.randomUUID().toString(),
                    name = customerName,
                    phone = customerPhone,
                    createdAt = now
                )
                db.customerDao().upsert(newCust)
                resolvedCustomerId = newCust.id
            }

            val sale = SaleEntity(
                id = saleId,
                invoiceNumber = invoiceNumber,
                customerId = resolvedCustomerId,
                customerName = customerName.ifBlank { "নগদ কাস্টমার" },
                customerPhone = customerPhone,
                subtotal = subtotal,
                discount = discount,
                totalAmount = totalAmount,
                paidAmount = effectivePaid,
                dueAmount = dueAmount,
                totalCostOfGoods = totalCogs,
                paymentMethod = paymentMethod,
                note = note,
                timestamp = now,
                isReturned = false
            )
            db.saleDao().upsertSale(sale)

            val itemEntities = lines.map { line ->
                SaleItemEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    productId = line.product.id,
                    productName = line.product.name,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    costPrice = line.product.purchasePrice,
                    lineTotal = line.unitPrice * line.quantity
                )
            }
            db.saleDao().insertItems(itemEntities)

            for (line in lines) {
                val current = db.productDao().getById(line.product.id) ?: continue
                val newStock = (current.currentStock - line.quantity).coerceAtLeast(0.0)
                val updatedProd = current.copy(
                    currentStock = newStock,
                    totalSoldQty = current.totalSoldQty + line.quantity,
                    updatedAt = now
                )
                db.productDao().upsert(updatedProd)
                if (newStock <= updatedProd.minStock) {
                    lowStockAlerts.add(updatedProd)
                }
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = current.id,
                        productName = current.name,
                        movementType = "SALE",
                        quantityDelta = -line.quantity,
                        stockAfter = newStock,
                        note = invoiceNumber,
                        referenceId = saleId,
                        timestamp = now
                    )
                )
            }

            if (effectivePaid > 0.0) {
                val account = if (paymentMethod == "DUE") "CASH" else paymentMethod
                db.cashTransactionDao().upsert(
                    CashTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "IN",
                        category = "SALE_INCOME",
                        accountType = account,
                        amount = effectivePaid,
                        description = "$invoiceNumber - ${sale.customerName}",
                        referenceId = saleId,
                        timestamp = now
                    )
                )
            }

            if (resolvedCustomerId != null) {
                val cust = db.customerDao().getById(resolvedCustomerId)
                if (cust != null) {
                    db.customerDao().upsert(
                        cust.copy(
                            totalPurchaseAmount = cust.totalPurchaseAmount + totalAmount,
                            totalPaidAmount = cust.totalPaidAmount + effectivePaid,
                            currentDue = cust.currentDue + dueAmount
                        )
                    )
                }
                if (dueAmount > 0.0) {
                    db.dueTransactionDao().insert(
                        DueTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            partyType = "CUSTOMER",
                            partyId = resolvedCustomerId,
                            partyName = sale.customerName,
                            partyPhone = sale.customerPhone,
                            txType = "DUE_ADDED",
                            amount = dueAmount,
                            paymentMethod = paymentMethod,
                            note = invoiceNumber,
                            referenceSaleId = saleId,
                            timestamp = now
                        )
                    )
                }
            }

            logAudit("SALE", saleId, "CREATE", "$invoiceNumber (${sale.customerName})", totalAmount)
            sale
        }
        maybeAutoBackup()
        return createdSale to lowStockAlerts
    }

    suspend fun returnSale(saleId: String) {
        db.withTransaction {
            val sale = db.saleDao().getById(saleId) ?: return@withTransaction
            if (sale.isReturned) return@withTransaction
            val items = db.saleDao().getItemsForSale(saleId)
            val now = System.currentTimeMillis()

            for (item in items) {
                val prod = db.productDao().getById(item.productId) ?: continue
                val newStock = prod.currentStock + item.quantity
                db.productDao().upsert(
                    prod.copy(
                        currentStock = newStock,
                        totalSoldQty = (prod.totalSoldQty - item.quantity).coerceAtLeast(0.0),
                        totalReturnedQty = prod.totalReturnedQty + item.quantity,
                        updatedAt = now
                    )
                )
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = prod.id,
                        productName = prod.name,
                        movementType = "SALE_RETURN",
                        quantityDelta = item.quantity,
                        stockAfter = newStock,
                        note = "Return ${sale.invoiceNumber}",
                        referenceId = saleId,
                        timestamp = now
                    )
                )
            }

            if (sale.paidAmount > 0.0) {
                val account = if (sale.paymentMethod == "DUE") "CASH" else sale.paymentMethod
                db.cashTransactionDao().upsert(
                    CashTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "OUT",
                        category = "SALE_RETURN_REFUND",
                        accountType = account,
                        amount = sale.paidAmount,
                        description = "Return ${sale.invoiceNumber}",
                        referenceId = "${saleId}_return",
                        timestamp = now
                    )
                )
            }

            sale.customerId?.let { cid ->
                val cust = db.customerDao().getById(cid)
                if (cust != null) {
                    db.customerDao().upsert(
                        cust.copy(
                            totalPurchaseAmount = (cust.totalPurchaseAmount - sale.totalAmount).coerceAtLeast(0.0),
                            totalPaidAmount = (cust.totalPaidAmount - sale.paidAmount).coerceAtLeast(0.0),
                            currentDue = (cust.currentDue - sale.dueAmount).coerceAtLeast(0.0)
                        )
                    )
                }
            }

            db.saleDao().upsertSale(sale.copy(isReturned = true))
            logAudit("SALE", saleId, "RETURN", "Returned ${sale.invoiceNumber}", -sale.totalAmount)
        }
        maybeAutoBackup()
    }

    suspend fun deleteSale(saleId: String) {
        db.withTransaction {
            val sale = db.saleDao().getById(saleId) ?: return@withTransaction
            val items = db.saleDao().getItemsForSale(saleId)
            val now = System.currentTimeMillis()

            if (!sale.isReturned) {
                for (item in items) {
                    val prod = db.productDao().getById(item.productId) ?: continue
                    val newStock = prod.currentStock + item.quantity
                    db.productDao().upsert(
                        prod.copy(
                            currentStock = newStock,
                            totalSoldQty = (prod.totalSoldQty - item.quantity).coerceAtLeast(0.0),
                            updatedAt = now
                        )
                    )
                }
                sale.customerId?.let { cid ->
                    val cust = db.customerDao().getById(cid)
                    if (cust != null) {
                        db.customerDao().upsert(
                            cust.copy(
                                totalPurchaseAmount = (cust.totalPurchaseAmount - sale.totalAmount).coerceAtLeast(0.0),
                                totalPaidAmount = (cust.totalPaidAmount - sale.paidAmount).coerceAtLeast(0.0),
                                currentDue = (cust.currentDue - sale.dueAmount).coerceAtLeast(0.0)
                            )
                        )
                    }
                }
            }

            db.cashTransactionDao().deleteByReferenceId(saleId)
            db.cashTransactionDao().deleteByReferenceId("${saleId}_return")
            db.dueTransactionDao().deleteBySaleId(saleId)
            db.stockMovementDao().deleteByReferenceId(saleId)
            db.saleDao().deleteItemsForSale(saleId)
            db.saleDao().deleteSaleById(saleId)
            logAudit("SALE", saleId, "DELETE", "Deleted ${sale.invoiceNumber}", -sale.totalAmount)
        }
        maybeAutoBackup()
    }

    // --- PURCHASES ---
    suspend fun createPurchase(
        supplierId: String?,
        supplierName: String,
        supplierPhone: String,
        lines: List<PurchaseLineInput>,
        transportCost: Double,
        otherCost: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ): PurchaseEntity {
        val created = db.withTransaction {
            val purchaseId = UUID.randomUUID().toString()
            val invoiceNumber = "PUR-${(timestamp % 1000000).toString().padStart(6, '0')}"

            val subtotal = lines.sumOf { it.unitCost * it.quantity }
            val totalAmount = subtotal + transportCost + otherCost
            val effectivePaid = if (paymentMethod == "DUE") 0.0 else paidAmount.coerceIn(0.0, totalAmount)
            val dueAmount = (totalAmount - effectivePaid).coerceAtLeast(0.0)

            var resolvedSupplierId = supplierId
            if (resolvedSupplierId == null && dueAmount > 0 && supplierName.isNotBlank()) {
                val newSupp = SupplierEntity(
                    id = UUID.randomUUID().toString(),
                    name = supplierName,
                    phone = supplierPhone,
                    createdAt = timestamp
                )
                db.supplierDao().upsert(newSupp)
                resolvedSupplierId = newSupp.id
            }

            val purchase = PurchaseEntity(
                id = purchaseId,
                invoiceNumber = invoiceNumber,
                supplierId = resolvedSupplierId,
                supplierName = supplierName.ifBlank { "সাপ্লায়ার" },
                supplierPhone = supplierPhone,
                productSubtotal = subtotal,
                transportCost = transportCost,
                otherCost = otherCost,
                totalAmount = totalAmount,
                paidAmount = effectivePaid,
                dueAmount = dueAmount,
                paymentMethod = paymentMethod,
                note = note,
                timestamp = timestamp
            )
            db.purchaseDao().upsertPurchase(purchase)

            val itemEntities = lines.map { line ->
                PurchaseItemEntity(
                    id = UUID.randomUUID().toString(),
                    purchaseId = purchaseId,
                    productId = line.product.id,
                    productName = line.product.name,
                    quantity = line.quantity,
                    unitCost = line.unitCost,
                    lineTotal = line.unitCost * line.quantity
                )
            }
            db.purchaseDao().insertItems(itemEntities)

            for (line in lines) {
                val prod = db.productDao().getById(line.product.id) ?: continue
                val newStock = prod.currentStock + line.quantity
                db.productDao().upsert(
                    prod.copy(
                        currentStock = newStock,
                        purchasePrice = line.unitCost,
                        totalPurchasedQty = prod.totalPurchasedQty + line.quantity,
                        updatedAt = timestamp
                    )
                )
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = prod.id,
                        productName = prod.name,
                        movementType = "PURCHASE",
                        quantityDelta = line.quantity,
                        stockAfter = newStock,
                        note = invoiceNumber,
                        referenceId = purchaseId,
                        timestamp = timestamp
                    )
                )
            }

            if (effectivePaid > 0.0) {
                val account = if (paymentMethod == "DUE") "CASH" else paymentMethod
                db.cashTransactionDao().upsert(
                    CashTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "OUT",
                        category = "PURCHASE_PAYMENT",
                        accountType = account,
                        amount = effectivePaid,
                        description = "$invoiceNumber - ${purchase.supplierName}",
                        referenceId = purchaseId,
                        timestamp = timestamp
                    )
                )
            }

            if (resolvedSupplierId != null) {
                val supp = db.supplierDao().getById(resolvedSupplierId)
                if (supp != null) {
                    db.supplierDao().upsert(
                        supp.copy(
                            totalPurchaseAmount = supp.totalPurchaseAmount + totalAmount,
                            totalPaidAmount = supp.totalPaidAmount + effectivePaid,
                            currentPayable = supp.currentPayable + dueAmount
                        )
                    )
                }
                if (dueAmount > 0.0) {
                    db.dueTransactionDao().insert(
                        DueTransactionEntity(
                            id = UUID.randomUUID().toString(),
                            partyType = "SUPPLIER",
                            partyId = resolvedSupplierId,
                            partyName = purchase.supplierName,
                            partyPhone = purchase.supplierPhone,
                            txType = "DUE_ADDED",
                            amount = dueAmount,
                            paymentMethod = paymentMethod,
                            note = invoiceNumber,
                            referencePurchaseId = purchaseId,
                            timestamp = timestamp
                        )
                    )
                }
            }

            logAudit("PURCHASE", purchaseId, "CREATE", "$invoiceNumber (${purchase.supplierName})", totalAmount)
            purchase
        }
        maybeAutoBackup()
        return created
    }

    suspend fun deletePurchase(purchaseId: String) {
        db.withTransaction {
            val purchase = db.purchaseDao().getById(purchaseId) ?: return@withTransaction
            val items = db.purchaseDao().getItemsForPurchase(purchaseId)
            val now = System.currentTimeMillis()

            for (item in items) {
                val prod = db.productDao().getById(item.productId) ?: continue
                db.productDao().upsert(
                    prod.copy(
                        currentStock = (prod.currentStock - item.quantity).coerceAtLeast(0.0),
                        totalPurchasedQty = (prod.totalPurchasedQty - item.quantity).coerceAtLeast(0.0),
                        updatedAt = now
                    )
                )
            }

            purchase.supplierId?.let { sid ->
                val supp = db.supplierDao().getById(sid)
                if (supp != null) {
                    db.supplierDao().upsert(
                        supp.copy(
                            totalPurchaseAmount = (supp.totalPurchaseAmount - purchase.totalAmount).coerceAtLeast(0.0),
                            totalPaidAmount = (supp.totalPaidAmount - purchase.paidAmount).coerceAtLeast(0.0),
                            currentPayable = (supp.currentPayable - purchase.dueAmount).coerceAtLeast(0.0)
                        )
                    )
                }
            }

            db.cashTransactionDao().deleteByReferenceId(purchaseId)
            db.dueTransactionDao().deleteByPurchaseId(purchaseId)
            db.stockMovementDao().deleteByReferenceId(purchaseId)
            db.purchaseDao().deleteItemsForPurchase(purchaseId)
            db.purchaseDao().deletePurchaseById(purchaseId)
            logAudit("PURCHASE", purchaseId, "DELETE", "Deleted ${purchase.invoiceNumber}", -purchase.totalAmount)
        }
        maybeAutoBackup()
    }

    // --- CUSTOMERS, SUPPLIERS & DUE BOOK ---
    suspend fun upsertCustomer(customer: CustomerEntity, isNew: Boolean) {
        db.withTransaction {
            val entity = if (isNew) customer.copy(id = customer.id.ifBlank { UUID.randomUUID().toString() }) else customer
            db.customerDao().upsert(entity)
            logAudit("CUSTOMER", entity.id, if (isNew) "CREATE" else "UPDATE", entity.name, entity.currentDue)
        }
        maybeAutoBackup()
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        db.withTransaction {
            db.customerDao().deleteById(customer.id)
            logAudit("CUSTOMER", customer.id, "DELETE", customer.name, 0.0)
        }
        maybeAutoBackup()
    }

    suspend fun upsertSupplier(supplier: SupplierEntity, isNew: Boolean) {
        db.withTransaction {
            val entity = if (isNew) supplier.copy(id = supplier.id.ifBlank { UUID.randomUUID().toString() }) else supplier
            db.supplierDao().upsert(entity)
            logAudit("SUPPLIER", entity.id, if (isNew) "CREATE" else "UPDATE", entity.name, entity.currentPayable)
        }
        maybeAutoBackup()
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) {
        db.withTransaction {
            db.supplierDao().deleteById(supplier.id)
            logAudit("SUPPLIER", supplier.id, "DELETE", supplier.name, 0.0)
        }
        maybeAutoBackup()
    }

    suspend fun recordCustomerDueOrCollection(
        customerId: String,
        isCollection: Boolean,
        amount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0) return
        db.withTransaction {
            val customer = db.customerDao().getById(customerId) ?: return@withTransaction
            val txId = UUID.randomUUID().toString()
            if (isCollection) {
                db.customerDao().upsert(
                    customer.copy(
                        totalPaidAmount = customer.totalPaidAmount + amount,
                        currentDue = (customer.currentDue - amount).coerceAtLeast(0.0)
                    )
                )
                db.dueTransactionDao().insert(
                    DueTransactionEntity(
                        id = txId,
                        partyType = "CUSTOMER",
                        partyId = customerId,
                        partyName = customer.name,
                        partyPhone = customer.phone,
                        txType = "COLLECTION",
                        amount = amount,
                        paymentMethod = paymentMethod,
                        note = note,
                        timestamp = timestamp
                    )
                )
                db.cashTransactionDao().upsert(
                    CashTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "IN",
                        category = "DUE_COLLECTION",
                        accountType = paymentMethod,
                        amount = amount,
                        description = "Due collected: ${customer.name}",
                        referenceId = txId,
                        timestamp = timestamp
                    )
                )
                logAudit("DUE", txId, "CREATE", "Collected from ${customer.name}", amount)
            } else {
                db.customerDao().upsert(
                    customer.copy(
                        totalPurchaseAmount = customer.totalPurchaseAmount + amount,
                        currentDue = customer.currentDue + amount
                    )
                )
                db.dueTransactionDao().insert(
                    DueTransactionEntity(
                        id = txId,
                        partyType = "CUSTOMER",
                        partyId = customerId,
                        partyName = customer.name,
                        partyPhone = customer.phone,
                        txType = "DUE_ADDED",
                        amount = amount,
                        paymentMethod = "DUE",
                        note = note,
                        timestamp = timestamp
                    )
                )
                logAudit("DUE", txId, "CREATE", "Added due for ${customer.name}", amount)
            }
        }
        maybeAutoBackup()
    }

    suspend fun recordSupplierDueOrPayment(
        supplierId: String,
        isPayment: Boolean,
        amount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0) return
        db.withTransaction {
            val supplier = db.supplierDao().getById(supplierId) ?: return@withTransaction
            val txId = UUID.randomUUID().toString()
            if (isPayment) {
                db.supplierDao().upsert(
                    supplier.copy(
                        totalPaidAmount = supplier.totalPaidAmount + amount,
                        currentPayable = (supplier.currentPayable - amount).coerceAtLeast(0.0)
                    )
                )
                db.dueTransactionDao().insert(
                    DueTransactionEntity(
                        id = txId,
                        partyType = "SUPPLIER",
                        partyId = supplierId,
                        partyName = supplier.name,
                        partyPhone = supplier.phone,
                        txType = "PAYMENT",
                        amount = amount,
                        paymentMethod = paymentMethod,
                        note = note,
                        timestamp = timestamp
                    )
                )
                db.cashTransactionDao().upsert(
                    CashTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "OUT",
                        category = "SUPPLIER_PAYMENT",
                        accountType = paymentMethod,
                        amount = amount,
                        description = "Paid supplier: ${supplier.name}",
                        referenceId = txId,
                        timestamp = timestamp
                    )
                )
                logAudit("DUE", txId, "CREATE", "Paid ${supplier.name}", -amount)
            } else {
                db.supplierDao().upsert(
                    supplier.copy(
                        totalPurchaseAmount = supplier.totalPurchaseAmount + amount,
                        currentPayable = supplier.currentPayable + amount
                    )
                )
                db.dueTransactionDao().insert(
                    DueTransactionEntity(
                        id = txId,
                        partyType = "SUPPLIER",
                        partyId = supplierId,
                        partyName = supplier.name,
                        partyPhone = supplier.phone,
                        txType = "DUE_ADDED",
                        amount = amount,
                        paymentMethod = "DUE",
                        note = note,
                        timestamp = timestamp
                    )
                )
                logAudit("DUE", txId, "CREATE", "Added payable to ${supplier.name}", amount)
            }
        }
        maybeAutoBackup()
    }

    // --- CASHBOOK & EXPENSES ---
    suspend fun addCashTransaction(
        type: String,
        category: String,
        accountType: String,
        amount: Double,
        description: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0) return
        db.withTransaction {
            val txId = UUID.randomUUID().toString()
            db.cashTransactionDao().upsert(
                CashTransactionEntity(
                    id = txId,
                    type = type,
                    category = category,
                    accountType = accountType,
                    amount = amount,
                    description = description,
                    timestamp = timestamp
                )
            )
            logAudit("CASH", txId, "CREATE", "$category: $description", if (type == "IN") amount else -amount)
        }
        maybeAutoBackup()
    }

    suspend fun deleteCashTransaction(tx: CashTransactionEntity) {
        db.withTransaction {
            db.cashTransactionDao().deleteById(tx.id)
            logAudit("CASH", tx.id, "DELETE", "Deleted cash entry: ${tx.description}", -tx.amount)
        }
        maybeAutoBackup()
    }

    suspend fun upsertExpense(expense: ExpenseEntity, isNew: Boolean) {
        db.withTransaction {
            val id = if (isNew) UUID.randomUUID().toString() else expense.id
            val saved = expense.copy(id = id)
            db.expenseDao().upsert(saved)
            val existingCash = db.cashTransactionDao().getByReferenceId(id)
            val cashTx = CashTransactionEntity(
                id = existingCash?.id ?: UUID.randomUUID().toString(),
                type = "OUT",
                category = "EXPENSE",
                accountType = saved.paymentMethod,
                amount = saved.amount,
                description = "${saved.title} (${saved.category})",
                referenceId = id,
                timestamp = saved.timestamp
            )
            db.cashTransactionDao().upsert(cashTx)
            logAudit("EXPENSE", id, if (isNew) "CREATE" else "UPDATE", saved.title, -saved.amount)
        }
        maybeAutoBackup()
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.withTransaction {
            db.expenseDao().deleteById(expense.id)
            db.cashTransactionDao().deleteByReferenceId(expense.id)
            logAudit("EXPENSE", expense.id, "DELETE", "Deleted expense: ${expense.title}", expense.amount)
        }
        maybeAutoBackup()
    }

    // --- AUDIT LOG ---
    private suspend fun logAudit(
        entityType: String,
        entityId: String,
        action: String,
        summary: String,
        amountImpact: Double
    ) {
        db.auditLogDao().insert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                amountImpact = amountImpact,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun logSecurityAudit(summary: String) {
        db.withTransaction {
            logAudit("SECURITY", "settings", "SECURITY", summary, 0.0)
        }
    }

    // --- BACKUP & RESTORE (JSON) ---
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settings = settingsRepository.settingsFlow.first()
        root.put("settings", JSONObject().apply {
            put("shopName", settings.shopName)
            put("ownerName", settings.ownerName)
            put("shopPhone", settings.shopPhone)
            put("shopAddress", settings.shopAddress)
            put("currency", settings.currency.code)
            put("language", settings.language.code)
            put("openingCash", settings.openingCashBalance)
            put("openingBank", settings.openingBankBalance)
            put("openingWallet", settings.openingWalletBalance)
        })

        root.put("products", JSONArray().apply {
            db.productDao().getAll().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("code", p.code)
                    put("barcode", p.barcode); put("category", p.category)
                    put("purchasePrice", p.purchasePrice); put("sellingPrice", p.sellingPrice)
                    put("currentStock", p.currentStock); put("minStock", p.minStock)
                    put("unit", p.unit); put("description", p.description); put("imageUri", p.imageUri)
                    put("totalPurchasedQty", p.totalPurchasedQty); put("totalSoldQty", p.totalSoldQty)
                    put("totalDamagedQty", p.totalDamagedQty); put("totalReturnedQty", p.totalReturnedQty)
                    put("createdAt", p.createdAt); put("updatedAt", p.updatedAt)
                })
            }
        })

        root.put("customers", JSONArray().apply {
            db.customerDao().getAll().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("phone", c.phone)
                    put("address", c.address); put("totalPurchaseAmount", c.totalPurchaseAmount)
                    put("totalPaidAmount", c.totalPaidAmount); put("currentDue", c.currentDue)
                    put("createdAt", c.createdAt)
                })
            }
        })

        root.put("suppliers", JSONArray().apply {
            db.supplierDao().getAll().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("phone", s.phone)
                    put("address", s.address); put("totalPurchaseAmount", s.totalPurchaseAmount)
                    put("totalPaidAmount", s.totalPaidAmount); put("currentPayable", s.currentPayable)
                    put("createdAt", s.createdAt)
                })
            }
        })

        root.put("sales", JSONArray().apply {
            db.saleDao().getAll().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("invoiceNumber", s.invoiceNumber)
                    put("customerId", s.customerId ?: ""); put("customerName", s.customerName)
                    put("customerPhone", s.customerPhone); put("subtotal", s.subtotal)
                    put("discount", s.discount); put("totalAmount", s.totalAmount)
                    put("paidAmount", s.paidAmount); put("dueAmount", s.dueAmount)
                    put("totalCostOfGoods", s.totalCostOfGoods); put("paymentMethod", s.paymentMethod)
                    put("note", s.note); put("timestamp", s.timestamp); put("isReturned", s.isReturned)
                })
            }
        })

        root.put("saleItems", JSONArray().apply {
            db.saleDao().getAllItems().forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("saleId", i.saleId); put("productId", i.productId)
                    put("productName", i.productName); put("quantity", i.quantity)
                    put("unitPrice", i.unitPrice); put("costPrice", i.costPrice)
                    put("lineTotal", i.lineTotal)
                })
            }
        })

        root.put("purchases", JSONArray().apply {
            db.purchaseDao().getAll().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("invoiceNumber", p.invoiceNumber)
                    put("supplierId", p.supplierId ?: ""); put("supplierName", p.supplierName)
                    put("supplierPhone", p.supplierPhone); put("productSubtotal", p.productSubtotal)
                    put("transportCost", p.transportCost); put("otherCost", p.otherCost)
                    put("totalAmount", p.totalAmount); put("paidAmount", p.paidAmount)
                    put("dueAmount", p.dueAmount); put("paymentMethod", p.paymentMethod)
                    put("note", p.note); put("timestamp", p.timestamp); put("isReturned", p.isReturned)
                })
            }
        })

        root.put("purchaseItems", JSONArray().apply {
            db.purchaseDao().getAllItems().forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id); put("purchaseId", i.purchaseId); put("productId", i.productId)
                    put("productName", i.productName); put("quantity", i.quantity)
                    put("unitCost", i.unitCost); put("lineTotal", i.lineTotal)
                })
            }
        })

        root.put("dueTransactions", JSONArray().apply {
            db.dueTransactionDao().getAll().forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("partyType", d.partyType); put("partyId", d.partyId)
                    put("partyName", d.partyName); put("partyPhone", d.partyPhone)
                    put("txType", d.txType); put("amount", d.amount)
                    put("paymentMethod", d.paymentMethod); put("note", d.note)
                    put("referenceSaleId", d.referenceSaleId ?: "")
                    put("referencePurchaseId", d.referencePurchaseId ?: "")
                    put("timestamp", d.timestamp)
                })
            }
        })

        root.put("cashTransactions", JSONArray().apply {
            db.cashTransactionDao().getAll().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("type", c.type); put("category", c.category)
                    put("accountType", c.accountType); put("amount", c.amount)
                    put("description", c.description); put("referenceId", c.referenceId ?: "")
                    put("timestamp", c.timestamp)
                })
            }
        })

        root.put("expenses", JSONArray().apply {
            db.expenseDao().getAll().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("title", e.title); put("category", e.category)
                    put("amount", e.amount); put("paymentMethod", e.paymentMethod)
                    put("note", e.note); put("receiptUri", e.receiptUri); put("timestamp", e.timestamp)
                })
            }
        })

        root.put("stockMovements", JSONArray().apply {
            db.stockMovementDao().getAll().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("productId", m.productId); put("productName", m.productName)
                    put("movementType", m.movementType); put("quantityDelta", m.quantityDelta)
                    put("stockAfter", m.stockAfter); put("note", m.note)
                    put("referenceId", m.referenceId ?: ""); put("timestamp", m.timestamp)
                })
            }
        })

        settingsRepository.recordBackupTimestamp()
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String) {
        val root = JSONObject(jsonString)
        val productsList = mutableListOf<ProductEntity>()
        root.optJSONArray("products")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                productsList.add(
                    ProductEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        code = o.optString("code"),
                        barcode = o.optString("barcode"),
                        category = o.optString("category", "সাধারণ"),
                        purchasePrice = o.optDouble("purchasePrice", 0.0),
                        sellingPrice = o.optDouble("sellingPrice", 0.0),
                        currentStock = o.optDouble("currentStock", 0.0),
                        minStock = o.optDouble("minStock", 5.0),
                        unit = o.optString("unit", "পিস"),
                        description = o.optString("description"),
                        imageUri = o.optString("imageUri"),
                        totalPurchasedQty = o.optDouble("totalPurchasedQty", 0.0),
                        totalSoldQty = o.optDouble("totalSoldQty", 0.0),
                        totalDamagedQty = o.optDouble("totalDamagedQty", 0.0),
                        totalReturnedQty = o.optDouble("totalReturnedQty", 0.0),
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt")
                    )
                )
            }
        }

        val customersList = mutableListOf<CustomerEntity>()
        root.optJSONArray("customers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                customersList.add(
                    CustomerEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone"),
                        address = o.optString("address"),
                        totalPurchaseAmount = o.optDouble("totalPurchaseAmount", 0.0),
                        totalPaidAmount = o.optDouble("totalPaidAmount", 0.0),
                        currentDue = o.optDouble("currentDue", 0.0),
                        createdAt = o.optLong("createdAt")
                    )
                )
            }
        }

        val suppliersList = mutableListOf<SupplierEntity>()
        root.optJSONArray("suppliers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                suppliersList.add(
                    SupplierEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        phone = o.optString("phone"),
                        address = o.optString("address"),
                        totalPurchaseAmount = o.optDouble("totalPurchaseAmount", 0.0),
                        totalPaidAmount = o.optDouble("totalPaidAmount", 0.0),
                        currentPayable = o.optDouble("currentPayable", 0.0),
                        createdAt = o.optLong("createdAt")
                    )
                )
            }
        }

        val salesList = mutableListOf<SaleEntity>()
        root.optJSONArray("sales")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                salesList.add(
                    SaleEntity(
                        id = o.getString("id"),
                        invoiceNumber = o.getString("invoiceNumber"),
                        customerId = o.optString("customerId").ifBlank { null },
                        customerName = o.optString("customerName"),
                        customerPhone = o.optString("customerPhone"),
                        subtotal = o.optDouble("subtotal", 0.0),
                        discount = o.optDouble("discount", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        paidAmount = o.optDouble("paidAmount", 0.0),
                        dueAmount = o.optDouble("dueAmount", 0.0),
                        totalCostOfGoods = o.optDouble("totalCostOfGoods", 0.0),
                        paymentMethod = o.optString("paymentMethod", "CASH"),
                        note = o.optString("note"),
                        timestamp = o.optLong("timestamp"),
                        isReturned = o.optBoolean("isReturned", false)
                    )
                )
            }
        }

        val saleItemsList = mutableListOf<SaleItemEntity>()
        root.optJSONArray("saleItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                saleItemsList.add(
                    SaleItemEntity(
                        id = o.getString("id"),
                        saleId = o.getString("saleId"),
                        productId = o.getString("productId"),
                        productName = o.optString("productName"),
                        quantity = o.optDouble("quantity", 0.0),
                        unitPrice = o.optDouble("unitPrice", 0.0),
                        costPrice = o.optDouble("costPrice", 0.0),
                        lineTotal = o.optDouble("lineTotal", 0.0)
                    )
                )
            }
        }

        val purchasesList = mutableListOf<PurchaseEntity>()
        root.optJSONArray("purchases")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                purchasesList.add(
                    PurchaseEntity(
                        id = o.getString("id"),
                        invoiceNumber = o.getString("invoiceNumber"),
                        supplierId = o.optString("supplierId").ifBlank { null },
                        supplierName = o.optString("supplierName"),
                        supplierPhone = o.optString("supplierPhone"),
                        productSubtotal = o.optDouble("productSubtotal", 0.0),
                        transportCost = o.optDouble("transportCost", 0.0),
                        otherCost = o.optDouble("otherCost", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        paidAmount = o.optDouble("paidAmount", 0.0),
                        dueAmount = o.optDouble("dueAmount", 0.0),
                        paymentMethod = o.optString("paymentMethod", "CASH"),
                        note = o.optString("note"),
                        timestamp = o.optLong("timestamp"),
                        isReturned = o.optBoolean("isReturned", false)
                    )
                )
            }
        }

        val purchaseItemsList = mutableListOf<PurchaseItemEntity>()
        root.optJSONArray("purchaseItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                purchaseItemsList.add(
                    PurchaseItemEntity(
                        id = o.getString("id"),
                        purchaseId = o.getString("purchaseId"),
                        productId = o.getString("productId"),
                        productName = o.optString("productName"),
                        quantity = o.optDouble("quantity", 0.0),
                        unitCost = o.optDouble("unitCost", 0.0),
                        lineTotal = o.optDouble("lineTotal", 0.0)
                    )
                )
            }
        }

        val dueList = mutableListOf<DueTransactionEntity>()
        root.optJSONArray("dueTransactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dueList.add(
                    DueTransactionEntity(
                        id = o.getString("id"),
                        partyType = o.getString("partyType"),
                        partyId = o.getString("partyId"),
                        partyName = o.optString("partyName"),
                        partyPhone = o.optString("partyPhone"),
                        txType = o.getString("txType"),
                        amount = o.optDouble("amount", 0.0),
                        paymentMethod = o.optString("paymentMethod", "CASH"),
                        note = o.optString("note"),
                        referenceSaleId = o.optString("referenceSaleId").ifBlank { null },
                        referencePurchaseId = o.optString("referencePurchaseId").ifBlank { null },
                        timestamp = o.optLong("timestamp")
                    )
                )
            }
        }

        val cashList = mutableListOf<CashTransactionEntity>()
        root.optJSONArray("cashTransactions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cashList.add(
                    CashTransactionEntity(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        category = o.getString("category"),
                        accountType = o.optString("accountType", "CASH"),
                        amount = o.optDouble("amount", 0.0),
                        description = o.optString("description"),
                        referenceId = o.optString("referenceId").ifBlank { null },
                        timestamp = o.optLong("timestamp")
                    )
                )
            }
        }

        val expensesList = mutableListOf<ExpenseEntity>()
        root.optJSONArray("expenses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                expensesList.add(
                    ExpenseEntity(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        category = o.optString("category", "OTHER"),
                        amount = o.optDouble("amount", 0.0),
                        paymentMethod = o.optString("paymentMethod", "CASH"),
                        note = o.optString("note"),
                        receiptUri = o.optString("receiptUri"),
                        timestamp = o.optLong("timestamp")
                    )
                )
            }
        }

        val stockMovementsList = mutableListOf<StockMovementEntity>()
        root.optJSONArray("stockMovements")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                stockMovementsList.add(
                    StockMovementEntity(
                        id = o.getString("id"),
                        productId = o.getString("productId"),
                        productName = o.optString("productName"),
                        movementType = o.getString("movementType"),
                        quantityDelta = o.optDouble("quantityDelta", 0.0),
                        stockAfter = o.optDouble("stockAfter", 0.0),
                        note = o.optString("note"),
                        referenceId = o.optString("referenceId").ifBlank { null },
                        timestamp = o.optLong("timestamp")
                    )
                )
            }
        }

        db.withTransaction {
            db.saleDao().clearAllItems()
            db.saleDao().clearAllSales()
            db.purchaseDao().clearAllItems()
            db.purchaseDao().clearAllPurchases()
            db.dueTransactionDao().clearAll()
            db.cashTransactionDao().clearAll()
            db.expenseDao().clearAll()
            db.stockMovementDao().clearAll()
            db.customerDao().clearAll()
            db.supplierDao().clearAll()
            db.productDao().clearAll()

            db.productDao().insertAll(productsList)
            db.customerDao().insertAll(customersList)
            db.supplierDao().insertAll(suppliersList)
            db.saleDao().insertAllSales(salesList)
            db.saleDao().insertItems(saleItemsList)
            db.purchaseDao().insertAllPurchases(purchasesList)
            db.purchaseDao().insertItems(purchaseItemsList)
            db.dueTransactionDao().insertAll(dueList)
            db.cashTransactionDao().insertAll(cashList)
            db.expenseDao().insertAll(expensesList)
            db.stockMovementDao().insertAll(stockMovementsList)

            logAudit("BACKUP", "restore", "RESTORE", "Restored ${productsList.size} products, ${salesList.size} sales", 0.0)
        }

        root.optJSONObject("settings")?.let { s ->
            settingsRepository.updateShopProfile(
                shopName = s.optString("shopName", "আমার দোকান"),
                ownerName = s.optString("ownerName", ""),
                phone = s.optString("shopPhone", ""),
                address = s.optString("shopAddress", ""),
                logoUri = ""
            )
            settingsRepository.updateOpeningBalances(
                cash = s.optDouble("openingCash", 0.0),
                bank = s.optDouble("openingBank", 0.0),
                wallet = s.optDouble("openingWallet", 0.0)
            )
        }
    }

    private fun autoBackupFile(): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        return File(dir, "auto_backup.json")
    }

    fun hasAutoBackup(): Boolean = autoBackupFile().exists()

    suspend fun saveAutoBackupNow() {
        val json = exportBackupJson()
        autoBackupFile().writeText(json)
    }

    suspend fun restoreFromAutoBackup(): Boolean {
        val file = autoBackupFile()
        if (!file.exists()) return false
        importBackupJson(file.readText())
        return true
    }

    private suspend fun maybeAutoBackup() {
        val settings = settingsRepository.settingsFlow.first()
        if (settings.autoBackupEnabled) {
            runCatching {
                val json = exportBackupJson()
                autoBackupFile().writeText(json)
            }
        }
    }

    companion object {
        fun dayRangeMillis(timeMillis: Long): Pair<Long, Long> {
            val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            return start to end
        }
    }
}
