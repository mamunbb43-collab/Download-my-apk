package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CashTransactionEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.BusinessRepository
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.*

@Composable
fun CashbookScreen(
    cashTransactions: List<CashTransactionEntity>,
    settings: ShopSettings,
    onAddCashTransaction: (type: String, category: String, accountType: String, amount: Double, description: String) -> Unit,
    onDeleteCashTransaction: (CashTransactionEntity) -> Unit,
    onUpdateOpeningBalances: (cash: Double, bank: Double, wallet: Double) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali

    var selectedFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showOpeningDialog by remember { mutableStateOf(false) }

    val cashBalance = remember(cashTransactions, settings.openingCashBalance) {
        settings.openingCashBalance + cashTransactions.filter { it.accountType == "CASH" }
            .sumOf { if (it.type == "IN") it.amount else -it.amount }
    }
    val bankBalance = remember(cashTransactions, settings.openingBankBalance) {
        settings.openingBankBalance + cashTransactions.filter { it.accountType == "BANK" }
            .sumOf { if (it.type == "IN") it.amount else -it.amount }
    }
    val walletBalance = remember(cashTransactions, settings.openingWalletBalance) {
        settings.openingWalletBalance + cashTransactions.filter { it.accountType == "MOBILE_WALLET" }
            .sumOf { if (it.type == "IN") it.amount else -it.amount }
    }

    val (dayStart, dayEnd) = BusinessRepository.dayRangeMillis(System.currentTimeMillis())
    val todayTxs = remember(cashTransactions, dayStart, dayEnd) {
        cashTransactions.filter { it.timestamp in dayStart..dayEnd }
    }
    val todayIn = todayTxs.filter { it.type == "IN" }.sumOf { it.amount }
    val todayOut = todayTxs.filter { it.type == "OUT" }.sumOf { it.amount }

    val filtered = remember(cashTransactions, selectedFilter) {
        if (selectedFilter == "ALL") cashTransactions else cashTransactions.filter { it.accountType == selectedFilter }
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(
                title = strings.cashbook,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showOpeningDialog = true }) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = strings.openingBalance)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.add) },
                containerColor = TileGreen,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricStatCard(
                        title = strings.cash,
                        value = Formatters.formatMoney(cashBalance, currency, isBn),
                        icon = Icons.Default.Payments,
                        accentColor = TileGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "${strings.bank} + ${strings.mobileWallet}",
                        value = Formatters.formatMoney(bankBalance + walletBalance, currency, isBn),
                        icon = Icons.Default.AccountBalance,
                        accentColor = TileCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.dailyClosingReport, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showOpeningDialog = true }) {
                                Text(strings.openingBalance)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.cashIn, color = ProfitGreen)
                            Text(Formatters.formatMoney(todayIn, currency, isBn), color = ProfitGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.cashOut, color = LossRed)
                            Text(Formatters.formatMoney(todayOut, currency, isBn), color = LossRed, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.closingBalance, fontWeight = FontWeight.Bold)
                            Text(
                                Formatters.formatMoney(cashBalance + bankBalance + walletBalance, currency, isBn),
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to strings.all,
                        "CASH" to strings.cash,
                        "BANK" to strings.bank,
                        "MOBILE_WALLET" to strings.mobileWallet
                    ).forEach { (k, l) ->
                        FilterChip(selected = selectedFilter == k, onClick = { selectedFilter = k }, label = { Text(l) })
                    }
                }
            }

            if (filtered.isEmpty()) {
                item { EmptyStateView(icon = Icons.Default.Calculate, message = strings.noData) }
            } else {
                items(filtered, key = { it.id }) { tx ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(tx.description.ifBlank { tx.category }, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${tx.category} • ${tx.accountType} • ${Formatters.formatDate(tx.timestamp, datePattern, isBn)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (if (tx.type == "IN") "+ " else "- ") + Formatters.formatMoney(tx.amount, currency, isBn),
                                    color = if (tx.type == "IN") ProfitGreen else LossRed,
                                    fontWeight = FontWeight.Bold
                                )
                                if (tx.referenceId == null) {
                                    IconButton(onClick = { onDeleteCashTransaction(tx) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = LossRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var type by remember { mutableStateOf("IN") }
        var category by remember { mutableStateOf("OTHER_INCOME") }
        var account by remember { mutableStateOf("CASH") }
        var amountInput by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.cashbook) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == "IN",
                            onClick = { type = "IN"; category = "OTHER_INCOME" },
                            label = { Text(strings.cashIn) }
                        )
                        FilterChip(
                            selected = type == "OUT",
                            onClick = { type = "OUT"; category = "OTHER_EXPENSE" },
                            label = { Text(strings.cashOut) }
                        )
                    }
                    if (type == "IN") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = category == "OTHER_INCOME", onClick = { category = "OTHER_INCOME" }, label = { Text(strings.otherIncome) })
                            FilterChip(selected = category == "CAPITAL_INVESTMENT", onClick = { category = "CAPITAL_INVESTMENT" }, label = { Text(strings.capitalInvestment) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to strings.cash, "BANK" to strings.bank, "MOBILE_WALLET" to strings.mobileWallet).forEach { (k, l) ->
                            FilterChip(selected = account == k, onClick = { account = k }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(strings.note) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onAddCashTransaction(type, category, account, amt, desc)
                            showAddDialog = false
                        }
                    },
                    enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showOpeningDialog) {
        var cashText by remember { mutableStateOf(settings.openingCashBalance.toString()) }
        var bankText by remember { mutableStateOf(settings.openingBankBalance.toString()) }
        var walletText by remember { mutableStateOf(settings.openingWalletBalance.toString()) }
        AlertDialog(
            onDismissRequest = { showOpeningDialog = false },
            title = { Text(strings.openingBalance) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cashText, onValueChange = { cashText = it }, label = { Text(strings.cash) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = bankText, onValueChange = { bankText = it }, label = { Text(strings.bank) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = walletText, onValueChange = { walletText = it }, label = { Text(strings.mobileWallet) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateOpeningBalances(
                        cashText.toDoubleOrNull() ?: 0.0,
                        bankText.toDoubleOrNull() ?: 0.0,
                        walletText.toDoubleOrNull() ?: 0.0
                    )
                    showOpeningDialog = false
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showOpeningDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
