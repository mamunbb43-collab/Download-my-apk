package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.localization.LocalAppStrings
import com.example.ui.screens.*
import com.example.ui.viewmodel.MainViewModel

object Routes {
    const val HOME = "home"
    const val DUE_BOOK = "due_book"
    const val SALES = "sales"
    const val PRODUCTS = "products"
    const val PROFILE = "profile"
    const val PURCHASE = "purchase"
    const val CASHBOOK = "cashbook"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val STOCK = "stock"
    const val SMS_REMINDERS = "sms_reminders"
    const val BACKUP = "backup"
    const val FINANCIAL_OVERVIEW = "financial_overview"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val strings = LocalAppStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val financialOverview by viewModel.financialOverview.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val saleItems by viewModel.saleItems.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val dueTransactions by viewModel.dueTransactions.collectAsState()
    val cashTransactions by viewModel.cashTransactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val lastCreatedSale by viewModel.lastCreatedSale.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomTabs = remember(strings) {
        listOf(
            BottomNavTab(Routes.HOME, strings.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomNavTab(Routes.DUE_BOOK, strings.navDueBook, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_due_book"),
            BottomNavTab(Routes.SALES, strings.navSales, Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_sales"),
            BottomNavTab(Routes.PRODUCTS, strings.navProducts, Icons.AutoMirrored.Filled.FormatListBulleted, Icons.AutoMirrored.Outlined.FormatListBulleted, "nav_products"),
            BottomNavTab(Routes.PROFILE, strings.navProfile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
        )
    }

    val showBottomNav = currentRoute in bottomTabs.map { it.route }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail && showBottomNav) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Spacer(Modifier.height(12.dp))
                    bottomTabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavRail && showBottomNav) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            bottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            settings = settings,
                            metrics = metrics,
                            onNavigateToSales = { navigateTopLevel(Routes.SALES) },
                            onNavigateToProducts = { navigateTopLevel(Routes.PRODUCTS) },
                            onNavigateToPurchase = { navController.navigate(Routes.PURCHASE) },
                            onNavigateToCashbook = { navController.navigate(Routes.CASHBOOK) },
                            onNavigateToExpenses = { navController.navigate(Routes.EXPENSES) },
                            onNavigateToDueBook = { navigateTopLevel(Routes.DUE_BOOK) },
                            onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                            onNavigateToSmsReminder = { navController.navigate(Routes.SMS_REMINDERS) },
                            onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                            onNavigateToStock = { navController.navigate(Routes.STOCK) },
                            onNavigateToFinancialOverview = { navController.navigate(Routes.FINANCIAL_OVERVIEW) }
                        )
                    }
                    composable(Routes.DUE_BOOK) {
                        DueBookScreen(
                            customers = customers,
                            suppliers = suppliers,
                            dueTransactions = dueTransactions,
                            settings = settings,
                            focusSmsReminders = false,
                            onSaveCustomer = viewModel::saveCustomer,
                            onDeleteCustomer = viewModel::deleteCustomer,
                            onSaveSupplier = viewModel::saveSupplier,
                            onDeleteSupplier = viewModel::deleteSupplier,
                            onCustomerTx = viewModel::recordCustomerDueOrCollection,
                            onSupplierTx = viewModel::recordSupplierDueOrPayment
                        )
                    }
                    composable(Routes.SALES) {
                        SalesScreen(
                            products = products,
                            customers = customers,
                            sales = sales,
                            allSaleItems = saleItems,
                            settings = settings,
                            lastCreatedSale = lastCreatedSale,
                            onClearLastCreatedSale = viewModel::clearLastCreatedSale,
                            onCreateSale = viewModel::createSale,
                            onReturnSale = viewModel::returnSale,
                            onDeleteSale = viewModel::deleteSale,
                            onQuickAddProduct = { prod -> viewModel.saveProduct(prod, true) }
                        )
                    }
                    composable(Routes.PRODUCTS) {
                        ProductsAndStockScreen(
                            products = products,
                            stockMovements = stockMovements,
                            initialTab = 0,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onAdjustStock = viewModel::adjustStock
                        )
                    }
                    composable(Routes.STOCK) {
                        ProductsAndStockScreen(
                            products = products,
                            stockMovements = stockMovements,
                            initialTab = 1,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onAdjustStock = viewModel::adjustStock,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PURCHASE) {
                        PurchaseScreen(
                            products = products,
                            suppliers = suppliers,
                            purchases = purchases,
                            onCreatePurchase = viewModel::createPurchase,
                            onDeletePurchase = viewModel::deletePurchase,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.CASHBOOK) {
                        CashbookScreen(
                            cashTransactions = cashTransactions,
                            settings = settings,
                            onAddCashTransaction = viewModel::addCashTransaction,
                            onDeleteCashTransaction = viewModel::deleteCashTransaction,
                            onUpdateOpeningBalances = viewModel::updateOpeningBalances,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.EXPENSES) {
                        ExpensesScreen(
                            expenses = expenses,
                            onSaveExpense = viewModel::saveExpense,
                            onDeleteExpense = viewModel::deleteExpense,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.REPORTS) {
                        ReportsAndProfitLossScreen(
                            repository = viewModel.repository,
                            products = products,
                            customers = customers,
                            suppliers = suppliers,
                            sales = sales,
                            purchases = purchases,
                            expenses = expenses,
                            cashTransactions = cashTransactions,
                            settings = settings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.SMS_REMINDERS) {
                        DueBookScreen(
                            customers = customers,
                            suppliers = suppliers,
                            dueTransactions = dueTransactions,
                            settings = settings,
                            focusSmsReminders = true,
                            onSaveCustomer = viewModel::saveCustomer,
                            onDeleteCustomer = viewModel::deleteCustomer,
                            onSaveSupplier = viewModel::saveSupplier,
                            onDeleteSupplier = viewModel::deleteSupplier,
                            onCustomerTx = viewModel::recordCustomerDueOrCollection,
                            onSupplierTx = viewModel::recordSupplierDueOrPayment,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.BACKUP) {
                        BackupAndRestoreScreen(
                            settings = settings,
                            hasAutoBackup = viewModel.repository.hasAutoBackup(),
                            onExportBackupUri = viewModel::exportBackupToUri,
                            onImportBackupUri = viewModel::importBackupFromUri,
                            onSaveAutoBackupNow = viewModel::saveAutoBackupNow,
                            onRestoreAutoBackup = viewModel::restoreLastAutoBackup,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.FINANCIAL_OVERVIEW) {
                        FinancialOverviewScreen(
                            overview = financialOverview,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.PROFILE) {
                        ProfileAndSettingsScreen(
                            settings = settings,
                            auditLogs = auditLogs,
                            onUpdateShopProfile = viewModel::updateShopProfile,
                            onUpdateCurrency = viewModel::updateCurrency,
                            onUpdateLanguage = viewModel::updateLanguage,
                            onUpdateDateFormat = viewModel::updateDateFormat,
                            onUpdateDarkMode = viewModel::updateDarkMode,
                            onUpdateSecurity = viewModel::updateSecurity,
                            onUpdateBackupSettings = viewModel::updateBackupSettings,
                            onNavigateToFinancialOverview = { navController.navigate(Routes.FINANCIAL_OVERVIEW) },
                            onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                            onLockAppNow = viewModel::lockAppNow
                        )
                    }
                }
            }
        }
    }
}
