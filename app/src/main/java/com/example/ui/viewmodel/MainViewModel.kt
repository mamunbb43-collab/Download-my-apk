package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.preferences.LockType
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.ShopSettings
import com.example.data.repository.*
import com.example.localization.AppCurrency
import com.example.localization.AppLanguage
import com.example.localization.DateFormatOption
import com.example.utils.ShareAndMediaHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val settingsRepository = SettingsRepository(application)
    val repository = BusinessRepository(application, db, settingsRepository)

    val settings: StateFlow<ShopSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ShopSettings())

    val dashboardMetrics: StateFlow<DashboardMetrics> = repository.observeDashboardMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val financialOverview: StateFlow<FinancialOverview> = repository.observeFinancialOverview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialOverview())

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.suppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleEntity>> = repository.sales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val saleItems: StateFlow<List<SaleItemEntity>> = repository.saleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseEntity>> = repository.purchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseItems: StateFlow<List<PurchaseItemEntity>> = repository.purchaseItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueTransactions: StateFlow<List<DueTransactionEntity>> = repository.dueTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashTransactions: StateFlow<List<CashTransactionEntity>> = repository.cashTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.expenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.stockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _lastCreatedSale = MutableStateFlow<Pair<SaleEntity, List<SaleItemEntity>>?>(null)
    val lastCreatedSale: StateFlow<Pair<SaleEntity, List<SaleItemEntity>>?> = _lastCreatedSale.asStateFlow()

    fun unlockWithSecret(secret: String): Boolean {
        val ok = settingsRepository.verifySecret(secret, settings.value.lockSecretHash)
        if (ok) {
            _isUnlocked.value = true
        }
        return ok
    }

    fun unlockViaBiometric() {
        _isUnlocked.value = true
    }

    fun lockAppNow() {
        if (settings.value.lockType != LockType.NONE) {
            _isUnlocked.value = false
        }
    }

    fun emitMessage(msg: String) {
        _snackbarMessage.tryEmit(msg)
    }

    // --- PRODUCTS & STOCK ---
    fun saveProduct(product: ProductEntity, isNew: Boolean) {
        viewModelScope.launch {
            repository.upsertProduct(product, isNew)
            emitMessage(if (settings.value.language == AppLanguage.BN) "পণ্য সংরক্ষিত হয়েছে" else "Product saved")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            emitMessage(if (settings.value.language == AppLanguage.BN) "পণ্য ডিলিট করা হয়েছে" else "Product deleted")
        }
    }

    fun adjustStock(productId: String, movementType: String, quantityDelta: Double, note: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, movementType, quantityDelta, note)
            emitMessage(if (settings.value.language == AppLanguage.BN) "স্টক আপডেট হয়েছে" else "Stock updated")
        }
    }

    // --- SALES ---
    fun createSale(
        customerId: String?,
        customerName: String,
        customerPhone: String,
        lines: List<CartLineInput>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ) {
        viewModelScope.launch {
            val (sale, lowStockItems) = repository.createSale(
                customerId = customerId,
                customerName = customerName,
                customerPhone = customerPhone,
                lines = lines,
                discount = discount,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                note = note
            )
            val items = db.saleDao().getItemsForSale(sale.id)
            _lastCreatedSale.value = sale to items
            if (settings.value.lowStockNotificationsEnabled && lowStockItems.isNotEmpty()) {
                ShareAndMediaHelper.notifyLowStock(
                    getApplication(),
                    lowStockItems,
                    settings.value.language == AppLanguage.BN
                )
            }
            emitMessage(if (settings.value.language == AppLanguage.BN) "বিক্রয় সম্পন্ন: ${sale.invoiceNumber}" else "Sale completed: ${sale.invoiceNumber}")
        }
    }

    fun clearLastCreatedSale() {
        _lastCreatedSale.value = null
    }

    fun returnSale(saleId: String) {
        viewModelScope.launch {
            repository.returnSale(saleId)
            emitMessage(if (settings.value.language == AppLanguage.BN) "বিক্রয় ফেরত প্রক্রিয়া সম্পন্ন" else "Sale returned")
        }
    }

    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            repository.deleteSale(saleId)
            emitMessage(if (settings.value.language == AppLanguage.BN) "বিক্রয় মুছে ফেলা হয়েছে ও হিসাব আপডেট হয়েছে" else "Sale deleted and ledger recalculated")
        }
    }

    // --- PURCHASES ---
    fun createPurchase(
        supplierId: String?,
        supplierName: String,
        supplierPhone: String,
        lines: List<PurchaseLineInput>,
        transportCost: Double,
        otherCost: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long
    ) {
        viewModelScope.launch {
            val pur = repository.createPurchase(
                supplierId = supplierId,
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                lines = lines,
                transportCost = transportCost,
                otherCost = otherCost,
                paidAmount = paidAmount,
                paymentMethod = paymentMethod,
                note = note,
                timestamp = timestamp
            )
            emitMessage(if (settings.value.language == AppLanguage.BN) "ক্রয় সম্পন্ন: ${pur.invoiceNumber}" else "Purchase saved: ${pur.invoiceNumber}")
        }
    }

    fun deletePurchase(purchaseId: String) {
        viewModelScope.launch {
            repository.deletePurchase(purchaseId)
            emitMessage(if (settings.value.language == AppLanguage.BN) "ক্রয় ডিলিট ও স্টক সমন্বয় হয়েছে" else "Purchase deleted and stock adjusted")
        }
    }

    // --- CUSTOMERS / SUPPLIERS / DUE BOOK ---
    fun saveCustomer(customer: CustomerEntity, isNew: Boolean) {
        viewModelScope.launch {
            repository.upsertCustomer(customer, isNew)
            emitMessage(if (settings.value.language == AppLanguage.BN) "কাস্টমার সংরক্ষিত" else "Customer saved")
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    fun saveSupplier(supplier: SupplierEntity, isNew: Boolean) {
        viewModelScope.launch {
            repository.upsertSupplier(supplier, isNew)
            emitMessage(if (settings.value.language == AppLanguage.BN) "সাপ্লায়ার সংরক্ষিত" else "Supplier saved")
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    fun recordCustomerDueOrCollection(
        customerId: String,
        isCollection: Boolean,
        amount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.recordCustomerDueOrCollection(customerId, isCollection, amount, paymentMethod, note, timestamp)
            emitMessage(if (settings.value.language == AppLanguage.BN) "বাকি খাতা আপডেট হয়েছে" else "Due ledger updated")
        }
    }

    fun recordSupplierDueOrPayment(
        supplierId: String,
        isPayment: Boolean,
        amount: Double,
        paymentMethod: String,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.recordSupplierDueOrPayment(supplierId, isPayment, amount, paymentMethod, note, timestamp)
            emitMessage(if (settings.value.language == AppLanguage.BN) "সাপ্লায়ার হিসাব আপডেট হয়েছে" else "Supplier ledger updated")
        }
    }

    // --- CASHBOOK & EXPENSES ---
    fun addCashTransaction(
        type: String,
        category: String,
        accountType: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            repository.addCashTransaction(type, category, accountType, amount, description)
            emitMessage(if (settings.value.language == AppLanguage.BN) "ক্যাশবুক এন্ট্রি যোগ হয়েছে" else "Cashbook entry saved")
        }
    }

    fun deleteCashTransaction(tx: CashTransactionEntity) {
        viewModelScope.launch {
            repository.deleteCashTransaction(tx)
        }
    }

    fun saveExpense(expense: ExpenseEntity, isNew: Boolean) {
        viewModelScope.launch {
            repository.upsertExpense(expense, isNew)
            emitMessage(if (settings.value.language == AppLanguage.BN) "খরচ সংরক্ষিত হয়েছে" else "Expense saved")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            emitMessage(if (settings.value.language == AppLanguage.BN) "খরচ ডিলিট ও ক্যাশ সমন্বয় হয়েছে" else "Expense deleted")
        }
    }

    // --- SETTINGS & BACKUP ---
    fun updateShopProfile(name: String, owner: String, phone: String, address: String, logoUri: String) {
        viewModelScope.launch {
            settingsRepository.updateShopProfile(name, owner, phone, address, logoUri)
            emitMessage(if (settings.value.language == AppLanguage.BN) "প্রোফাইল আপডেট হয়েছে" else "Profile updated")
        }
    }

    fun updateCurrency(currency: AppCurrency) {
        viewModelScope.launch { settingsRepository.updateCurrency(currency) }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.updateLanguage(language) }
    }

    fun updateDateFormat(format: DateFormatOption) {
        viewModelScope.launch { settingsRepository.updateDateFormat(format) }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDarkMode(enabled) }
    }

    fun updateOpeningBalances(cash: Double, bank: Double, wallet: Double) {
        viewModelScope.launch {
            settingsRepository.updateOpeningBalances(cash, bank, wallet)
            emitMessage(if (settings.value.language == AppLanguage.BN) "শুরুর ব্যালেন্স আপডেট হয়েছে" else "Opening balances updated")
        }
    }

    fun updateSecurity(lockType: LockType, rawSecret: String?, biometricEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSecurity(lockType, rawSecret, biometricEnabled)
            repository.logSecurityAudit("Security updated: $lockType, biometric=$biometricEnabled")
            if (lockType == LockType.NONE) {
                _isUnlocked.value = true
            }
            emitMessage(if (settings.value.language == AppLanguage.BN) "নিরাপত্তা সেটিংস সংরক্ষিত" else "Security settings saved")
        }
    }

    fun updateBackupSettings(autoBackup: Boolean, lowStockNotif: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBackupSettings(autoBackup, lowStockNotif)
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportBackupJson()
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                emitMessage(if (settings.value.language == AppLanguage.BN) "JSON ব্যাকআপ সফলভাবে সেভ হয়েছে" else "JSON backup exported successfully")
            }.onFailure {
                emitMessage(if (settings.value.language == AppLanguage.BN) "ব্যাকআপ ব্যর্থ হয়েছে" else "Backup export failed")
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Empty file")
                repository.importBackupJson(json)
            }.onSuccess {
                emitMessage(if (settings.value.language == AppLanguage.BN) "ডাটা সফলভাবে রিস্টোর হয়েছে" else "Data restored successfully")
            }.onFailure {
                emitMessage(if (settings.value.language == AppLanguage.BN) "ফাইল থেকে রিস্টোর করা যায়নি" else "Failed to restore backup file")
            }
        }
    }

    fun saveAutoBackupNow() {
        viewModelScope.launch {
            repository.saveAutoBackupNow()
            emitMessage(if (settings.value.language == AppLanguage.BN) "লোকাল ব্যাকআপ তৈরি হয়েছে" else "Local backup created")
        }
    }

    fun restoreLastAutoBackup() {
        viewModelScope.launch {
            val ok = runCatching { repository.restoreFromAutoBackup() }.getOrDefault(false)
            emitMessage(
                if (ok) {
                    if (settings.value.language == AppLanguage.BN) "সর্বশেষ অটো-ব্যাকআপ থেকে ডাটা উদ্ধার হয়েছে" else "Recovered from auto-backup"
                } else {
                    if (settings.value.language == AppLanguage.BN) "কোনো অটো-ব্যাকআপ পাওয়া যায়নি" else "No auto-backup found"
                }
            )
        }
    }
}
