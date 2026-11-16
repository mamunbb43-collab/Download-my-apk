package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.preferences.SettingsRepository
import com.example.data.repository.BusinessRepository
import com.example.data.repository.CartLineInput
import com.example.data.repository.PurchaseLineInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BusinessRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var repo: BusinessRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsRepo = SettingsRepository(context)
        repo = BusinessRepository(context, db, settingsRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sale updates stock cashbook due and recalculates cleanly on delete`() = runTest {
        val product = ProductEntity(
            id = "p1",
            name = "Miniket Rice",
            purchasePrice = 60.0,
            sellingPrice = 75.0,
            currentStock = 20.0,
            minStock = 5.0,
            unit = "kg"
        )
        repo.upsertProduct(product, isNew = true)

        val customer = CustomerEntity(id = "c1", name = "Karim", phone = "01700000000")
        repo.upsertCustomer(customer, isNew = true)

        // Create partial-paid sale: 10 kg * 75 = 750 total, 500 paid, 250 due
        val (sale, _) = repo.createSale(
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            lines = listOf(CartLineInput(product, 10.0, 75.0)),
            discount = 0.0,
            paidAmount = 500.0,
            paymentMethod = "CASH",
            note = "Test"
        )

        val updatedProd = db.productDao().getById("p1")!!
        assertEquals(10.0, updatedProd.currentStock, 0.001)
        assertEquals(10.0, updatedProd.totalSoldQty, 0.001)

        val updatedCust = db.customerDao().getById("c1")!!
        assertEquals(750.0, updatedCust.totalPurchaseAmount, 0.001)
        assertEquals(500.0, updatedCust.totalPaidAmount, 0.001)
        assertEquals(250.0, updatedCust.currentDue, 0.001)

        // Only 500 should appear in cashbook (not 750)
        val cashTxs = repo.cashTransactions.first()
        assertEquals(1, cashTxs.size)
        assertEquals(500.0, cashTxs.first().amount, 0.001)

        // Check Profit & Loss: Gross = 750 - 600 = 150
        val pnl = repo.computeProfitLoss(
            repo.sales.first(),
            cashTxs,
            repo.expenses.first(),
            0L,
            Long.MAX_VALUE
        )
        assertEquals(750.0, pnl.totalSales, 0.001)
        assertEquals(600.0, pnl.totalCogs, 0.001)
        assertEquals(150.0, pnl.grossProfit, 0.001)

        // Delete sale -> stock, cash, and customer due must revert cleanly
        repo.deleteSale(sale.id)
        assertEquals(20.0, db.productDao().getById("p1")!!.currentStock, 0.001)
        assertEquals(0.0, db.customerDao().getById("c1")!!.currentDue, 0.001)
        assertTrue(repo.cashTransactions.first().isEmpty())
    }

    @Test
    fun `purchase expense and json backup restore preserve all data`() = runTest {
        val product = ProductEntity(
            id = "p2",
            name = "Soybean Oil",
            purchasePrice = 150.0,
            sellingPrice = 175.0,
            currentStock = 5.0,
            unit = "L"
        )
        repo.upsertProduct(product, isNew = true)

        repo.createPurchase(
            supplierId = null,
            supplierName = "Dhaka Traders",
            supplierPhone = "01811111111",
            lines = listOf(PurchaseLineInput(product, 10.0, 155.0)),
            transportCost = 50.0,
            otherCost = 0.0,
            paidAmount = 1000.0,
            paymentMethod = "CASH",
            note = "Restock"
        )

        val prodAfter = db.productDao().getById("p2")!!
        assertEquals(15.0, prodAfter.currentStock, 0.001)
        assertEquals(155.0, prodAfter.purchasePrice, 0.001)

        repo.upsertExpense(
            ExpenseEntity(id = "", title = "Electricity Bill", category = "ELECTRICITY", amount = 300.0),
            isNew = true
        )
        assertEquals(1, repo.expenses.first().size)

        val json = repo.exportBackupJson()
        // Clear DB and restore from JSON
        db.productDao().clearAll()
        db.expenseDao().clearAll()
        assertTrue(repo.products.first().isEmpty())

        repo.importBackupJson(json)
        assertEquals(1, repo.products.first().size)
        assertEquals(15.0, repo.products.first().first().currentStock, 0.001)
        assertEquals(1, repo.expenses.first().size)
        assertEquals(300.0, repo.expenses.first().first().amount, 0.001)
    }
}
