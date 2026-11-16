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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.data.local.DueTransactionEntity
import com.example.data.local.SupplierEntity
import com.example.data.preferences.ShopSettings
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.utils.ShareAndMediaHelper

@Composable
fun DueBookScreen(
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    dueTransactions: List<DueTransactionEntity>,
    settings: ShopSettings,
    focusSmsReminders: Boolean = false,
    onSaveCustomer: (CustomerEntity, Boolean) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onSaveSupplier: (SupplierEntity, Boolean) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    onCustomerTx: (customerId: String, isCollection: Boolean, amount: Double, method: String, note: String) -> Unit,
    onSupplierTx: (supplierId: String, isPayment: Boolean, amount: Double, method: String, note: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddPartyDialog by remember { mutableStateOf(false) }
    var txDialogState by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) } // partyId, partyName, isSettle
    var statementTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // partyId, partyName

    val totalCustomerDue = customers.sumOf { it.currentDue }
    val totalSupplierPayable = suppliers.sumOf { it.currentPayable }

    Scaffold(
        topBar = {
            AmarDokanTopBar(
                title = if (focusSmsReminders) strings.qaSmsReminder else strings.qaDueBook,
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPartyDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text(if (selectedTab == 0) strings.addCustomer else strings.addSupplier) },
                containerColor = TilePurple,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_party_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("${strings.customers} (${Formatters.toLocalizedDigits(customers.size.toString(), isBn)})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.suppliers} (${Formatters.toLocalizedDigits(suppliers.size.toString(), isBn)})") }
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TilePurple)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedTab == 0) strings.customerReceivable else strings.supplierPayable,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = Formatters.formatMoney(
                                    if (selectedTab == 0) totalCustomerDue else totalSupplierPayable,
                                    currency,
                                    isBn
                                ),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(34.dp))
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.search) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedTab == 0) {
                val filtered = customers.filter {
                    val qOk = searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.phone.contains(searchQuery, true)
                    val smsOk = !focusSmsReminders || it.currentDue > 0
                    qOk && smsOk
                }
                if (filtered.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.People,
                        message = strings.noData,
                        actionLabel = strings.addCustomer,
                        onAction = { showAddPartyDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { cust ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(cust.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (cust.phone.isNotBlank()) {
                                                Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (cust.address.isNotBlank()) {
                                                Text(cust.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(strings.due, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = Formatters.formatMoney(cust.currentDue, currency, isBn),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (cust.currentDue > 0) LossRed else ProfitGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isBn) "মোট কেনাকাটা: ${Formatters.formatMoney(cust.totalPurchaseAmount, currency, true)} | পরিশোধ: ${Formatters.formatMoney(cust.totalPaidAmount, currency, true)}"
                                        else "Total Bought: ${Formatters.formatMoney(cust.totalPurchaseAmount, currency, false)} | Paid: ${Formatters.formatMoney(cust.totalPaidAmount, currency, false)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(onClick = { txDialogState = Triple(cust.id, cust.name, true) }) {
                                            Text(strings.collectDue)
                                        }
                                        OutlinedButton(onClick = { txDialogState = Triple(cust.id, cust.name, false) }) {
                                            Text(strings.addDue)
                                        }
                                        if (cust.currentDue > 0) {
                                            OutlinedButton(onClick = {
                                                ShareAndMediaHelper.sendSmsDueReminder(
                                                    context,
                                                    cust.phone,
                                                    cust.name,
                                                    cust.currentDue,
                                                    settings
                                                )
                                            }) {
                                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("SMS")
                                            }
                                        }
                                        TextButton(onClick = { statementTarget = cust.id to cust.name }) {
                                            Text(strings.statement)
                                        }
                                        IconButton(onClick = { onDeleteCustomer(cust) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = LossRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val filtered = suppliers.filter {
                    searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.phone.contains(searchQuery, true)
                }
                if (filtered.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.LocalShipping,
                        message = strings.noData,
                        actionLabel = strings.addSupplier,
                        onAction = { showAddPartyDialog = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { supp ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(supp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            if (supp.phone.isNotBlank()) {
                                                Text(supp.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(strings.supplierPayable, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = Formatters.formatMoney(supp.currentPayable, currency, isBn),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (supp.currentPayable > 0) WarningAmber else ProfitGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(onClick = { txDialogState = Triple(supp.id, supp.name, true) }) {
                                            Text(strings.paySupplier)
                                        }
                                        OutlinedButton(onClick = { txDialogState = Triple(supp.id, supp.name, false) }) {
                                            Text(strings.addDue)
                                        }
                                        TextButton(onClick = { statementTarget = supp.id to supp.name }) {
                                            Text(strings.statement)
                                        }
                                        IconButton(onClick = { onDeleteSupplier(supp) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = LossRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPartyDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var initialDue by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPartyDialog = false },
            title = { Text(if (selectedTab == 0) strings.addCustomer else strings.addSupplier) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (selectedTab == 0) strings.customerName else strings.supplierName) }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.phone) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, singleLine = true)
                    OutlinedTextField(value = initialDue, onValueChange = { initialDue = it }, label = { Text(strings.due) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val due = initialDue.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                        if (selectedTab == 0) {
                            onSaveCustomer(CustomerEntity(id = "", name = name.trim(), phone = phone.trim(), address = address.trim(), totalPurchaseAmount = due, currentDue = due), true)
                        } else {
                            onSaveSupplier(SupplierEntity(id = "", name = name.trim(), phone = phone.trim(), address = address.trim(), totalPurchaseAmount = due, currentPayable = due), true)
                        }
                        showAddPartyDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddPartyDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    txDialogState?.let { (partyId, partyName, isSettle) ->
        var amountInput by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("CASH") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { txDialogState = null },
            title = {
                val title = when {
                    selectedTab == 0 && isSettle -> "${strings.collectDue} — $partyName"
                    selectedTab == 0 -> "${strings.addDue} — $partyName"
                    isSettle -> "${strings.paySupplier} — $partyName"
                    else -> "${strings.addDue} — $partyName"
                }
                Text(title)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text(strings.amount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSettle) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("CASH" to strings.cash, "BANK" to strings.bank, "MOBILE_WALLET" to strings.mobileWallet).forEach { (k, l) ->
                                FilterChip(selected = method == k, onClick = { method = k }, label = { Text(l) })
                            }
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(strings.note) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (selectedTab == 0) {
                                onCustomerTx(partyId, isSettle, amt, method, note)
                            } else {
                                onSupplierTx(partyId, isSettle, amt, method, note)
                            }
                            txDialogState = null
                        }
                    },
                    enabled = (amountInput.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { txDialogState = null }) { Text(strings.cancel) }
            }
        )
    }

    statementTarget?.let { (partyId, partyName) ->
        val partyTxs = dueTransactions.filter { it.partyId == partyId }
        AlertDialog(
            onDismissRequest = { statementTarget = null },
            title = { Text("${strings.statement} — $partyName") },
            text = {
                if (partyTxs.isEmpty()) {
                    Text(strings.noData)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(partyTxs, key = { it.id }) { tx ->
                            val isCredit = tx.txType == "COLLECTION" || tx.txType == "PAYMENT"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(tx.txType, style = MaterialTheme.typography.titleSmall)
                                    Text(Formatters.formatDate(tx.timestamp, datePattern, isBn), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = (if (isCredit) "- " else "+ ") + Formatters.formatMoney(tx.amount, currency, isBn),
                                    color = if (isCredit) ProfitGreen else LossRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { statementTarget = null }) { Text(strings.close) }
            }
        )
    }
}
