package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.preferences.ShopSettings
import com.example.data.repository.BusinessRepository
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.theme.*
import com.example.utils.CsvExporter
import com.example.utils.PdfGenerator
import com.example.utils.ShareAndMediaHelper
import java.util.Calendar

private enum class ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
private enum class ReportCategory { PNL, SALES, PURCHASE, EXPENSE, STOCK, CUSTOMER_DUE, SUPPLIER_DUE, CASH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAndProfitLossScreen(
    repository: BusinessRepository,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    sales: List<SaleEntity>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    cashTransactions: List<CashTransactionEntity>,
    settings: ShopSettings,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali
    val context = LocalContext.current

    var period by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var category by remember { mutableStateOf(ReportCategory.PNL) }
    var customRange by remember { mutableStateOf(BusinessRepository.dayRangeMillis(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val (startMillis, endMillis) = remember(period, customRange) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis
        when (period) {
            ReportPeriod.DAILY -> BusinessRepository.dayRangeMillis(now)
            ReportPeriod.WEEKLY -> {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                c.timeInMillis to end
            }
            ReportPeriod.MONTHLY -> {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                c.timeInMillis to end
            }
            ReportPeriod.YEARLY -> {
                val c = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                }
                c.timeInMillis to end
            }
            ReportPeriod.CUSTOM -> customRange
        }
    }

    val pnl = remember(sales, cashTransactions, expenses, startMillis, endMillis) {
        repository.computeProfitLoss(sales, cashTransactions, expenses, startMillis, endMillis)
    }

    val filteredSales = remember(sales, startMillis, endMillis) {
        sales.filter { !it.isReturned && it.timestamp in startMillis..endMillis }
    }
    val filteredPurchases = remember(purchases, startMillis, endMillis) {
        purchases.filter { !it.isReturned && it.timestamp in startMillis..endMillis }
    }
    val filteredExpenses = remember(expenses, startMillis, endMillis) {
        expenses.filter { it.timestamp in startMillis..endMillis }
    }
    val filteredCash = remember(cashTransactions, startMillis, endMillis) {
        cashTransactions.filter { it.timestamp in startMillis..endMillis }
    }

    fun buildExportTable(): Triple<String, List<String>, List<List<String>>> {
        return when (category) {
            ReportCategory.PNL -> Triple(
                strings.profitAndLoss,
                listOf(if (isBn) "বিবরণ" else "Item", strings.amount),
                listOf(
                    listOf(strings.todayTotalSales, Formatters.formatMoney(pnl.totalSales, currency, false)),
                    listOf(strings.cogs, Formatters.formatMoney(pnl.totalCogs, currency, false)),
                    listOf(strings.grossProfit, Formatters.formatMoney(pnl.grossProfit, currency, false)),
                    listOf(strings.otherIncome, Formatters.formatMoney(pnl.otherIncome, currency, false)),
                    listOf(strings.operatingExpense, Formatters.formatMoney(pnl.operatingExpenses, currency, false)),
                    listOf(strings.netProfitOrLoss, Formatters.formatMoney(pnl.netProfit, currency, false))
                )
            )
            ReportCategory.SALES -> Triple(
                strings.salesHistory,
                listOf(strings.invoice, strings.customerName, strings.total, strings.paid, strings.due),
                filteredSales.map {
                    listOf(
                        it.invoiceNumber,
                        it.customerName,
                        Formatters.formatMoney(it.totalAmount, currency, false),
                        Formatters.formatMoney(it.paidAmount, currency, false),
                        Formatters.formatMoney(it.dueAmount, currency, false)
                    )
                }
            )
            ReportCategory.PURCHASE -> Triple(
                strings.purchaseHistory,
                listOf(strings.invoice, strings.supplierName, strings.total, strings.paid, strings.due),
                filteredPurchases.map {
                    listOf(
                        it.invoiceNumber,
                        it.supplierName,
                        Formatters.formatMoney(it.totalAmount, currency, false),
                        Formatters.formatMoney(it.paidAmount, currency, false),
                        Formatters.formatMoney(it.dueAmount, currency, false)
                    )
                }
            )
            ReportCategory.EXPENSE -> Triple(
                strings.qaShopExpenses,
                listOf(strings.expenseTitle, strings.category, strings.amount),
                filteredExpenses.map {
                    listOf(it.title, it.category, Formatters.formatMoney(it.amount, currency, false))
                }
            )
            ReportCategory.STOCK -> Triple(
                strings.stockManagement,
                listOf(strings.productName, strings.currentStock, strings.purchasePrice, strings.sellingPrice),
                products.map {
                    listOf(
                        it.name,
                        "${it.currentStock} ${it.unit}",
                        Formatters.formatMoney(it.purchasePrice, currency, false),
                        Formatters.formatMoney(it.sellingPrice, currency, false)
                    )
                }
            )
            ReportCategory.CUSTOMER_DUE -> Triple(
                strings.customers,
                listOf(strings.customerName, strings.phone, strings.total, strings.due),
                customers.map {
                    listOf(
                        it.name,
                        it.phone,
                        Formatters.formatMoney(it.totalPurchaseAmount, currency, false),
                        Formatters.formatMoney(it.currentDue, currency, false)
                    )
                }
            )
            ReportCategory.SUPPLIER_DUE -> Triple(
                strings.suppliers,
                listOf(strings.supplierName, strings.phone, strings.total, strings.supplierPayable),
                suppliers.map {
                    listOf(
                        it.name,
                        it.phone,
                        Formatters.formatMoney(it.totalPurchaseAmount, currency, false),
                        Formatters.formatMoney(it.currentPayable, currency, false)
                    )
                }
            )
            ReportCategory.CASH -> Triple(
                strings.cashbook,
                listOf(strings.note, strings.category, strings.paymentMethod, strings.amount),
                filteredCash.map {
                    listOf(
                        it.description,
                        it.category,
                        it.accountType,
                        "${if (it.type == "IN") "+" else "-"}${Formatters.formatMoney(it.amount, currency, false)}"
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(title = strings.reports, onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Period Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReportPeriod.DAILY to strings.daily,
                    ReportPeriod.WEEKLY to strings.weekly,
                    ReportPeriod.MONTHLY to strings.monthly,
                    ReportPeriod.YEARLY to strings.yearly,
                    ReportPeriod.CUSTOM to strings.customRange
                ).forEach { (p, label) ->
                    FilterChip(
                        selected = period == p,
                        onClick = {
                            if (p == ReportPeriod.CUSTOM) showDatePicker = true
                            period = p
                        },
                        label = { Text(label) }
                    )
                }
            }

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ReportCategory.PNL to strings.profitAndLoss,
                    ReportCategory.SALES to strings.qaSellProduct,
                    ReportCategory.PURCHASE to strings.qaPurchaseProduct,
                    ReportCategory.EXPENSE to strings.qaShopExpenses,
                    ReportCategory.STOCK to strings.stockManagement,
                    ReportCategory.CUSTOMER_DUE to strings.customers,
                    ReportCategory.SUPPLIER_DUE to strings.suppliers,
                    ReportCategory.CASH to strings.cashbook
                ).forEach { (c, label) ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(label) })
                }
            }

            // Export Row (PDF & Excel CSV)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val (title, headers, rows) = buildExportTable()
                        val subtitle = "${Formatters.formatDate(startMillis, datePattern, false)} - ${Formatters.formatDate(endMillis, datePattern, false)}"
                        val file = PdfGenerator.generateTableReportPdf(
                            context = context,
                            title = title,
                            subtitle = subtitle,
                            summaryLines = listOf(
                                strings.netProfitOrLoss to Formatters.formatMoney(pnl.netProfit, currency, false)
                            ),
                            headers = headers,
                            rows = rows,
                            settings = settings,
                            fileNamePrefix = "report_${category.name.lowercase()}"
                        )
                        ShareAndMediaHelper.openPdfFile(context, file)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportPdf)
                }
                OutlinedButton(
                    onClick = {
                        val (_, headers, rows) = buildExportTable()
                        val file = CsvExporter.generateCsvFile(
                            context = context,
                            headers = headers,
                            rows = rows,
                            fileNamePrefix = "report_${category.name.lowercase()}"
                        )
                        ShareAndMediaHelper.shareFile(context, file, "text/csv", strings.exportExcelCsv)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exportExcelCsv)
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (category) {
                    ReportCategory.PNL -> {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(strings.profitAndLoss, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    HorizontalDivider()
                                    PnlRow(strings.todayTotalSales, Formatters.formatMoney(pnl.totalSales, currency, isBn), TileBlue)
                                    PnlRow(strings.cogs, "- " + Formatters.formatMoney(pnl.totalCogs, currency, isBn), TileOrange)
                                    HorizontalDivider()
                                    PnlRow(strings.grossProfit, Formatters.formatMoney(pnl.grossProfit, currency, isBn), TealPrimary, bold = true)
                                    PnlRow(strings.otherIncome, "+ " + Formatters.formatMoney(pnl.otherIncome, currency, isBn), ProfitGreen)
                                    PnlRow(strings.operatingExpense, "- " + Formatters.formatMoney(pnl.operatingExpenses, currency, isBn), LossRed)
                                    HorizontalDivider()
                                    PnlRow(
                                        strings.netProfitOrLoss,
                                        Formatters.formatMoney(pnl.netProfit, currency, isBn),
                                        if (pnl.netProfit >= 0) ProfitGreen else LossRed,
                                        bold = true
                                    )
                                }
                            }
                        }
                    }
                    ReportCategory.SALES -> {
                        items(filteredSales, key = { it.id }) { s ->
                            ReportSimpleCard(
                                title = "${s.invoiceNumber} — ${s.customerName}",
                                subtitle = Formatters.formatDate(s.timestamp, datePattern, isBn),
                                value = Formatters.formatMoney(s.totalAmount, currency, isBn),
                                valueColor = TileBlue
                            )
                        }
                    }
                    ReportCategory.PURCHASE -> {
                        items(filteredPurchases, key = { it.id }) { p ->
                            ReportSimpleCard(
                                title = "${p.invoiceNumber} — ${p.supplierName}",
                                subtitle = Formatters.formatDate(p.timestamp, datePattern, isBn),
                                value = Formatters.formatMoney(p.totalAmount, currency, isBn),
                                valueColor = TileOrange
                            )
                        }
                    }
                    ReportCategory.EXPENSE -> {
                        items(filteredExpenses, key = { it.id }) { e ->
                            ReportSimpleCard(
                                title = e.title,
                                subtitle = "${e.category} • ${Formatters.formatDate(e.timestamp, datePattern, isBn)}",
                                value = Formatters.formatMoney(e.amount, currency, isBn),
                                valueColor = LossRed
                            )
                        }
                    }
                    ReportCategory.STOCK -> {
                        items(products, key = { it.id }) { prod ->
                            ReportSimpleCard(
                                title = prod.name,
                                subtitle = "${strings.currentStock}: ${Formatters.formatQty(prod.currentStock, isBn)} ${prod.unit}",
                                value = Formatters.formatMoney(prod.currentStock * prod.purchasePrice, currency, isBn),
                                valueColor = TealPrimary
                            )
                        }
                    }
                    ReportCategory.CUSTOMER_DUE -> {
                        items(customers, key = { it.id }) { c ->
                            ReportSimpleCard(
                                title = c.name,
                                subtitle = c.phone.ifBlank { "-" },
                                value = Formatters.formatMoney(c.currentDue, currency, isBn),
                                valueColor = if (c.currentDue > 0) LossRed else ProfitGreen
                            )
                        }
                    }
                    ReportCategory.SUPPLIER_DUE -> {
                        items(suppliers, key = { it.id }) { s ->
                            ReportSimpleCard(
                                title = s.name,
                                subtitle = s.phone.ifBlank { "-" },
                                value = Formatters.formatMoney(s.currentPayable, currency, isBn),
                                valueColor = if (s.currentPayable > 0) WarningAmber else ProfitGreen
                            )
                        }
                    }
                    ReportCategory.CASH -> {
                        items(filteredCash, key = { it.id }) { tx ->
                            ReportSimpleCard(
                                title = tx.description.ifBlank { tx.category },
                                subtitle = "${tx.accountType} • ${Formatters.formatDate(tx.timestamp, datePattern, isBn)}",
                                value = (if (tx.type == "IN") "+ " else "- ") + Formatters.formatMoney(tx.amount, currency, isBn),
                                valueColor = if (tx.type == "IN") ProfitGreen else LossRed
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val s = state.selectedStartDateMillis
                    val e = state.selectedEndDateMillis
                    if (s != null && e != null) {
                        customRange = s to (e + 86399999L)
                    }
                    showDatePicker = false
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DateRangePicker(state = state, modifier = Modifier.height(420.dp))
        }
    }
}

@Composable
private fun PnlRow(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReportSimpleCard(title: String, subtitle: String, value: String, valueColor: Color) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}
