package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseEntity
import com.example.data.local.SupplierEntity
import com.example.data.repository.PurchaseLineInput
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TileOrange

@Composable
fun PurchaseScreen(
    products: List<ProductEntity>,
    suppliers: List<SupplierEntity>,
    purchases: List<PurchaseEntity>,
    onCreatePurchase: (
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
    ) -> Unit,
    onDeletePurchase: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali

    var selectedTab by remember { mutableIntStateOf(0) }
    val purchaseLines = remember { mutableStateListOf<PurchaseLineInput>() }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var transportCostInput by remember { mutableStateOf("") }
    var otherCostInput by remember { mutableStateOf("") }
    var paidInput by remember { mutableStateOf("") }
    var paidEdited by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var noteInput by remember { mutableStateOf("") }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var searchProduct by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var scanFeedbackMsg by remember { mutableStateOf<String?>(null) }

    val subtotal = purchaseLines.sumOf { it.unitCost * it.quantity }
    val transportCost = transportCostInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val otherCost = otherCostInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalPurchase = subtotal + transportCost + otherCost

    LaunchedEffect(totalPurchase, paymentMethod) {
        if (paymentMethod == "DUE") {
            paidInput = "0"
        } else if (!paidEdited) {
            paidInput = if (totalPurchase > 0) totalPurchase.toLong().toString() else ""
        }
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(
                title = strings.qaPurchaseProduct,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                }
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.newPurchase) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("${strings.purchaseHistory} (${Formatters.toLocalizedDigits(purchases.size.toString(), isBn)})") })
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchProduct,
                        onValueChange = { searchProduct = it },
                        placeholder = { Text(strings.search) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchProduct.isNotEmpty()) {
                                IconButton(onClick = { searchProduct = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else {
                                IconButton(onClick = { showBarcodeScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purchase_scan_barcode_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TealPrimary.copy(alpha = 0.14f),
                            contentColor = TealPrimary
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "বারকোড স্ক্যান করে পণ্য যোগ করুন" else "Scan Barcode to Add Item",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    scanFeedbackMsg?.let { msg ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ProfitGreen.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                    Text(msg, style = MaterialTheme.typography.bodySmall, color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(onClick = { scanFeedbackMsg = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    val filtered = products.filter {
                        searchProduct.isBlank() ||
                            it.name.contains(searchProduct, true) ||
                            it.code.contains(searchProduct, true) ||
                            it.barcode.contains(searchProduct, true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyStateView(icon = Icons.Default.Inventory2, message = strings.noData)
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            filtered.take(25).forEach { prod ->
                                Card(
                                    onClick = {
                                        val idx = purchaseLines.indexOfFirst { it.product.id == prod.id }
                                        if (idx >= 0) {
                                            val old = purchaseLines[idx]
                                            purchaseLines[idx] = old.copy(quantity = old.quantity + 1.0)
                                        } else {
                                            purchaseLines.add(PurchaseLineInput(prod, 1.0, prod.purchasePrice))
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.width(150.dp)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(prod.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        Text(
                                            Formatters.formatMoney(prod.purchasePrice, currency, isBn),
                                            color = TileOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${strings.currentStock}: ${Formatters.formatQty(prod.currentStock, isBn)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (purchaseLines.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                purchaseLines.forEachIndexed { idx, line ->
                                    var qtyText by remember(line.product.id, line.quantity) {
                                        mutableStateOf(if (line.quantity % 1.0 == 0.0) line.quantity.toLong().toString() else line.quantity.toString())
                                    }
                                    var costText by remember(line.product.id, line.unitCost) {
                                        mutableStateOf(line.unitCost.toString())
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(line.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { purchaseLines.removeAt(idx) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = LossRed)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = qtyText,
                                                onValueChange = {
                                                    qtyText = it
                                                    it.toDoubleOrNull()?.let { q ->
                                                        if (q > 0) purchaseLines[idx] = line.copy(quantity = q)
                                                    }
                                                },
                                                label = { Text(strings.quantity) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = costText,
                                                onValueChange = {
                                                    costText = it
                                                    it.toDoubleOrNull()?.let { c ->
                                                        if (c >= 0) purchaseLines[idx] = line.copy(unitCost = c)
                                                    }
                                                },
                                                label = { Text(strings.purchasePrice) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    if (idx < purchaseLines.lastIndex) HorizontalDivider()
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(strings.suppliers, style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { showSupplierPicker = true }) {
                                        Text(if (isBn) "তালিকা থেকে বাছাই" else "Select Saved")
                                    }
                                }
                                OutlinedTextField(
                                    value = supplierName,
                                    onValueChange = { supplierName = it; selectedSupplier = null },
                                    label = { Text(strings.supplierName) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = supplierPhone,
                                    onValueChange = { supplierPhone = it },
                                    label = { Text(strings.phone) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = transportCostInput,
                                        onValueChange = { transportCostInput = it },
                                        label = { Text(strings.transportCost) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = otherCostInput,
                                        onValueChange = { otherCostInput = it },
                                        label = { Text(strings.otherCost) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "CASH" to strings.cash,
                                        "BANK" to strings.bank,
                                        "MOBILE_WALLET" to strings.mobileWallet,
                                        "DUE" to strings.dueMethod
                                    ).forEach { (k, label) ->
                                        FilterChip(
                                            selected = paymentMethod == k,
                                            onClick = {
                                                paymentMethod = k
                                                if (k != "DUE") paidEdited = false
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = paidInput,
                                    onValueChange = { paidEdited = true; paidInput = it },
                                    label = { Text(strings.paid) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    enabled = paymentMethod != "DUE",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val paidVal = if (paymentMethod == "DUE") 0.0 else (paidInput.toDoubleOrNull() ?: totalPurchase).coerceIn(0.0, totalPurchase)
                                val dueVal = (totalPurchase - paidVal).coerceAtLeast(0.0)

                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.total, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(Formatters.formatMoney(totalPurchase, currency, isBn), style = MaterialTheme.typography.titleLarge, color = TileOrange, fontWeight = FontWeight.Bold)
                                }
                                if (dueVal > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(strings.due, color = LossRed)
                                        Text(Formatters.formatMoney(dueVal, currency, isBn), color = LossRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedTextField(
                                    value = noteInput,
                                    onValueChange = { noteInput = it },
                                    label = { Text(strings.note) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onCreatePurchase(
                                            selectedSupplier?.id,
                                            supplierName,
                                            supplierPhone,
                                            purchaseLines.toList(),
                                            transportCost,
                                            otherCost,
                                            paidVal,
                                            paymentMethod,
                                            noteInput,
                                            System.currentTimeMillis()
                                        )
                                        purchaseLines.clear()
                                        selectedSupplier = null
                                        supplierName = ""
                                        supplierPhone = ""
                                        transportCostInput = ""
                                        otherCostInput = ""
                                        paidInput = ""
                                        paidEdited = false
                                        noteInput = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text(strings.completePurchase)
                                }
                            }
                        }
                    }
                }
            } else {
                if (purchases.isEmpty()) {
                    EmptyStateView(icon = Icons.Default.AddShoppingCart, message = strings.noData)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(purchases, key = { it.id }) { pur ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(pur.invoiceNumber, fontWeight = FontWeight.Bold)
                                        Text(
                                            Formatters.formatDate(pur.timestamp, datePattern, isBn),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(pur.supplierName)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${strings.total}: ${Formatters.formatMoney(pur.totalAmount, currency, isBn)}", fontWeight = FontWeight.SemiBold)
                                        if (pur.dueAmount > 0) {
                                            Text("${strings.due}: ${Formatters.formatMoney(pur.dueAmount, currency, isBn)}", color = LossRed)
                                        } else {
                                            Text(strings.paid, color = ProfitGreen)
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { onDeletePurchase(pur.id) }) {
                                            Text(strings.delete, color = LossRed)
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

    if (showSupplierPicker) {
        AlertDialog(
            onDismissRequest = { showSupplierPicker = false },
            title = { Text(strings.suppliers) },
            text = {
                if (suppliers.isEmpty()) Text(strings.noData)
                else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(suppliers, key = { it.id }) { supp ->
                            ListItem(
                                headlineContent = { Text(supp.name) },
                                supportingContent = { if (supp.phone.isNotBlank()) Text(supp.phone) },
                                modifier = Modifier.clickable {
                                    selectedSupplier = supp
                                    supplierName = supp.name
                                    supplierPhone = supp.phone
                                    showSupplierPicker = false
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupplierPicker = false }) { Text(strings.close) }
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { rawCode ->
                val code = rawCode.trim()
                showBarcodeScanner = false
                val match = products.find {
                    it.barcode.equals(code, ignoreCase = true) || it.code.equals(code, ignoreCase = true)
                }
                if (match != null) {
                    val idx = purchaseLines.indexOfFirst { it.product.id == match.id }
                    if (idx >= 0) {
                        val old = purchaseLines[idx]
                        purchaseLines[idx] = old.copy(quantity = old.quantity + 1.0)
                    } else {
                        purchaseLines.add(PurchaseLineInput(match, 1.0, match.purchasePrice))
                    }
                    searchProduct = ""
                    scanFeedbackMsg = if (isBn) {
                        "✓ ${match.name} ক্রয় তালিকায় যোগ হয়েছে"
                    } else {
                        "✓ ${match.name} added to purchase list"
                    }
                } else {
                    searchProduct = code
                    scanFeedbackMsg = if (isBn) {
                        "বারকোড '$code' এর কোনো পণ্য পাওয়া যায়নি"
                    } else {
                        "No product matched barcode '$code'"
                    }
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
