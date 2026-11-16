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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.CartLineInput
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
import com.example.ui.theme.TileBlue
import com.example.ui.theme.WarningAmber
import com.example.utils.PdfGenerator
import com.example.utils.ShareAndMediaHelper
import java.util.UUID

@Composable
fun SalesScreen(
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    sales: List<SaleEntity>,
    allSaleItems: List<SaleItemEntity>,
    settings: ShopSettings,
    lastCreatedSale: Pair<SaleEntity, List<SaleItemEntity>>?,
    onClearLastCreatedSale: () -> Unit,
    onCreateSale: (
        customerId: String?,
        customerName: String,
        customerPhone: String,
        lines: List<CartLineInput>,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ) -> Unit,
    onReturnSale: (String) -> Unit,
    onDeleteSale: (String) -> Unit,
    onQuickAddProduct: (ProductEntity) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var scanFeedbackMsg by remember { mutableStateOf<String?>(null) }
    var unmatchedBarcodeForQuickAdd by remember { mutableStateOf<String?>(null) }

    // Cart state
    val cartLines = remember { mutableStateListOf<CartLineInput>() }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var paidAmountInput by remember { mutableStateOf("") }
    var paidManuallyEdited by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    var showCustomerPicker by remember { mutableStateOf(false) }
    var viewingSale by remember { mutableStateOf<Pair<SaleEntity, List<SaleItemEntity>>?>(null) }
    var confirmDeleteSale by remember { mutableStateOf<SaleEntity?>(null) }

    val subtotal = cartLines.sumOf { it.unitPrice * it.quantity }
    val discount = discountInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalBill = (subtotal - discount).coerceAtLeast(0.0)

    LaunchedEffect(totalBill, paymentMethod) {
        if (paymentMethod == "DUE") {
            paidAmountInput = "0"
        } else if (!paidManuallyEdited) {
            paidAmountInput = if (totalBill > 0) totalBill.toLong().toString() else ""
        }
    }

    LaunchedEffect(lastCreatedSale) {
        if (lastCreatedSale != null) {
            viewingSale = lastCreatedSale
            cartLines.clear()
            selectedCustomer = null
            customerName = ""
            customerPhone = ""
            discountInput = ""
            paidAmountInput = ""
            paidManuallyEdited = false
            noteInput = ""
            onClearLastCreatedSale()
        }
    }

    fun addProductToCart(product: ProductEntity) {
        val idx = cartLines.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            val existing = cartLines[idx]
            cartLines[idx] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            cartLines.add(CartLineInput(product = product, quantity = 1.0, unitPrice = product.sellingPrice))
        }
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(
                title = strings.qaSellProduct,
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(strings.newSale) },
                    modifier = Modifier.testTag("tab_new_sale")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("${strings.salesHistory} (${Formatters.toLocalizedDigits(sales.size.toString(), isBn)})") },
                    modifier = Modifier.testTag("tab_sales_history")
                )
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Product Search + Barcode
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(strings.search) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else {
                                IconButton(onClick = { showBarcodeScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sales_product_search")
                    )

                    FilledTonalButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sales_scan_barcode_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TealPrimary.copy(alpha = 0.14f),
                            contentColor = TealPrimary
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "ক্যামেরা দিয়ে বারকোড স্ক্যান করুন" else "Scan Barcode with Camera",
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
                                TextButton(onClick = { showBarcodeScanner = true }) {
                                    Text(if (isBn) "আবার স্ক্যান" else "Scan Again")
                                }
                                IconButton(onClick = { scanFeedbackMsg = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = strings.close, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    val filteredProducts = remember(products, searchQuery) {
                        if (searchQuery.isBlank()) products
                        else products.filter {
                            it.name.contains(searchQuery, true) ||
                                it.code.contains(searchQuery, true) ||
                                it.barcode.contains(searchQuery, true)
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Inventory2,
                            message = if (isBn) "বিক্রয়ের জন্য প্রথমে পণ্যের তালিকা থেকে পণ্য যোগ করুন" else "Add products in Product List first to start selling"
                        )
                    } else {
                        Text(strings.selectProduct, style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            filteredProducts.take(25).forEach { prod ->
                                val outOfStock = prod.currentStock <= 0
                                Card(
                                    onClick = { addProductToCart(prod) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (outOfStock) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.width(156.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = Formatters.formatMoney(prod.sellingPrice, currency, isBn),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TileBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${strings.currentStock}: ${Formatters.formatQty(prod.currentStock, isBn)} ${prod.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (prod.currentStock <= prod.minStock) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cart Lines
                    if (cartLines.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "${strings.cartItems} (${Formatters.toLocalizedDigits(cartLines.size.toString(), isBn)})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                cartLines.forEachIndexed { idx, line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(line.product.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${Formatters.formatMoney(line.unitPrice, currency, isBn)} x ${Formatters.formatQty(line.quantity, isBn)} = ${Formatters.formatMoney(line.unitPrice * line.quantity, currency, isBn)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = {
                                                if (line.quantity > 1.0) {
                                                    cartLines[idx] = line.copy(quantity = line.quantity - 1.0)
                                                } else {
                                                    cartLines.removeAt(idx)
                                                }
                                            }) {
                                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                            }
                                            Text(
                                                Formatters.formatQty(line.quantity, isBn),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            IconButton(onClick = {
                                                cartLines[idx] = line.copy(quantity = line.quantity + 1.0)
                                            }) {
                                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                            }
                                        }
                                    }
                                    if (idx < cartLines.lastIndex) HorizontalDivider()
                                }
                            }
                        }

                        // Customer & Billing Section
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.customers, style = MaterialTheme.typography.titleSmall)
                                    TextButton(onClick = { showCustomerPicker = true }) {
                                        Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isBn) "তালিকা থেকে বাছাই" else "Select Saved")
                                    }
                                }

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = {
                                        customerName = it
                                        selectedCustomer = null
                                    },
                                    label = { Text("${strings.customerName} (${strings.walkInCustomer})") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text(strings.customerPhone) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = discountInput,
                                        onValueChange = { discountInput = it },
                                        label = { Text(strings.discount) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = paidAmountInput,
                                        onValueChange = {
                                            paidManuallyEdited = true
                                            paidAmountInput = it
                                        },
                                        label = { Text(strings.paid) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        enabled = paymentMethod != "DUE",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "CASH" to strings.cash,
                                        "BANK" to strings.bank,
                                        "MOBILE_WALLET" to strings.mobileWallet,
                                        "DUE" to strings.dueMethod
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = paymentMethod == key,
                                            onClick = {
                                                paymentMethod = key
                                                if (key != "DUE") paidManuallyEdited = false
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }

                                val paidVal = if (paymentMethod == "DUE") 0.0 else (paidAmountInput.toDoubleOrNull() ?: totalBill).coerceIn(0.0, totalBill)
                                val dueVal = (totalBill - paidVal).coerceAtLeast(0.0)

                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.subtotal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(Formatters.formatMoney(subtotal, currency, isBn))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.total, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        Formatters.formatMoney(totalBill, currency, isBn),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (dueVal > 0) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(strings.due, color = LossRed, fontWeight = FontWeight.SemiBold)
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
                                        onCreateSale(
                                            selectedCustomer?.id,
                                            customerName,
                                            customerPhone,
                                            cartLines.toList(),
                                            discount,
                                            paidVal,
                                            paymentMethod,
                                            noteInput
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("complete_sale_button")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.completeSale)
                                }
                            }
                        }
                    }
                }
            } else {
                // Sales History
                if (sales.isEmpty()) {
                    EmptyStateView(icon = Icons.Default.Receipt, message = strings.noData)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sales, key = { it.id }) { sale ->
                            val saleLineItems = remember(allSaleItems, sale.id) {
                                allSaleItems.filter { it.saleId == sale.id }
                            }
                            Card(
                                onClick = { viewingSale = sale to saleLineItems },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sale.invoiceNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        val badgeColor = when {
                                            sale.isReturned -> WarningAmber
                                            sale.dueAmount > 0 -> LossRed
                                            else -> ProfitGreen
                                        }
                                        val badgeText = when {
                                            sale.isReturned -> strings.returned
                                            sale.dueAmount > 0 -> "${strings.due}: ${Formatters.formatMoney(sale.dueAmount, currency, isBn)}"
                                            else -> strings.paid
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(badgeColor.copy(alpha = 0.14f))
                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                        ) {
                                            Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor)
                                        }
                                    }
                                    Text(sale.customerName, style = MaterialTheme.typography.bodyMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            Formatters.formatDate(sale.timestamp, datePattern, isBn),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            Formatters.formatMoney(sale.totalAmount, currency, isBn),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { viewingSale = sale to saleLineItems }) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.invoice)
                                        }
                                        if (!sale.isReturned) {
                                            TextButton(onClick = { onReturnSale(sale.id) }) {
                                                Text(strings.returnSale, color = WarningAmber)
                                            }
                                        }
                                        TextButton(onClick = { confirmDeleteSale = sale }) {
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

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { rawCode ->
                val code = rawCode.trim()
                showBarcodeScanner = false
                val match = products.find {
                    it.barcode.equals(code, ignoreCase = true) || it.code.equals(code, ignoreCase = true)
                }
                if (match != null) {
                    addProductToCart(match)
                    searchQuery = ""
                    scanFeedbackMsg = if (isBn) {
                        "✓ ${match.name} কার্টে যোগ হয়েছে"
                    } else {
                        "✓ ${match.name} added to cart"
                    }
                } else {
                    unmatchedBarcodeForQuickAdd = code
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    unmatchedBarcodeForQuickAdd?.let { scannedCode ->
        var quickName by remember(scannedCode) { mutableStateOf("") }
        var quickPurchasePrice by remember(scannedCode) { mutableStateOf("") }
        var quickSellingPrice by remember(scannedCode) { mutableStateOf("") }
        var quickStock by remember(scannedCode) { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { unmatchedBarcodeForQuickAdd = null },
            title = {
                Text(
                    if (isBn) "নতুন বারকোড পাওয়া গেছে" else "New Barcode Detected"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBn) {
                            "বারকোড '$scannedCode' এর কোনো পণ্য তালিকায় নেই। দ্রুত নতুন পণ্য তৈরি করে কার্টে যোগ করুন:"
                        } else {
                            "No product found with barcode '$scannedCode'. Quick-add it and add to cart:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = quickName,
                        onValueChange = { quickName = it },
                        label = { Text(strings.productName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_add_product_name")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quickPurchasePrice,
                            onValueChange = { quickPurchasePrice = it },
                            label = { Text(strings.purchasePrice) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quickSellingPrice,
                            onValueChange = { quickSellingPrice = it },
                            label = { Text(strings.sellingPrice) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = quickStock,
                        onValueChange = { quickStock = it },
                        label = { Text(strings.currentStock) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newProduct = ProductEntity(
                            id = UUID.randomUUID().toString(),
                            name = quickName.trim(),
                            barcode = scannedCode,
                            category = if (isBn) "সাধারণ" else "General",
                            purchasePrice = quickPurchasePrice.toDoubleOrNull() ?: 0.0,
                            sellingPrice = quickSellingPrice.toDoubleOrNull() ?: 0.0,
                            currentStock = quickStock.toDoubleOrNull() ?: 1.0,
                            minStock = 5.0,
                            unit = if (isBn) "পিস" else "pcs"
                        )
                        onQuickAddProduct(newProduct)
                        addProductToCart(newProduct)
                        scanFeedbackMsg = if (isBn) {
                            "✓ ${newProduct.name} তৈরি ও কার্টে যোগ হয়েছে"
                        } else {
                            "✓ ${newProduct.name} created & added to cart"
                        }
                        searchQuery = ""
                        unmatchedBarcodeForQuickAdd = null
                    },
                    enabled = quickName.isNotBlank() && (quickSellingPrice.toDoubleOrNull() ?: -1.0) >= 0.0
                ) {
                    Text(if (isBn) "যোগ ও কার্টে নিন" else "Save & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    searchQuery = scannedCode
                    unmatchedBarcodeForQuickAdd = null
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text(strings.customers) },
            text = {
                if (customers.isEmpty()) {
                    Text(strings.noData)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(customers, key = { it.id }) { cust ->
                            ListItem(
                                headlineContent = { Text(cust.name) },
                                supportingContent = {
                                    if (cust.phone.isNotBlank()) Text(cust.phone)
                                },
                                modifier = Modifier.clickable {
                                    selectedCustomer = cust
                                    customerName = cust.name
                                    customerPhone = cust.phone
                                    showCustomerPicker = false
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) { Text(strings.close) }
            }
        )
    }

    viewingSale?.let { (sale, items) ->
        AlertDialog(
            onDismissRequest = { viewingSale = null },
            title = { Text("${strings.invoice} — ${sale.invoiceNumber}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${strings.customerName}: ${sale.customerName}")
                    if (sale.customerPhone.isNotBlank()) {
                        Text("${strings.phone}: ${sale.customerPhone}")
                    }
                    Text("${strings.date}: ${Formatters.formatDate(sale.timestamp, datePattern, isBn)}")
                    HorizontalDivider()
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} x${Formatters.formatQty(item.quantity, isBn)}", modifier = Modifier.weight(1f))
                            Text(Formatters.formatMoney(item.lineTotal, currency, isBn))
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.total, fontWeight = FontWeight.Bold)
                        Text(Formatters.formatMoney(sale.totalAmount, currency, isBn), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.paid)
                        Text(Formatters.formatMoney(sale.paidAmount, currency, isBn), color = ProfitGreen)
                    }
                    if (sale.dueAmount > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.due)
                            Text(Formatters.formatMoney(sale.dueAmount, currency, isBn), color = LossRed)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val file = PdfGenerator.generateSaleInvoicePdf(context, sale, items, settings)
                                ShareAndMediaHelper.openPdfFile(context, file)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                ShareAndMediaHelper.shareSaleOnWhatsApp(context, sale, items, settings)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("WhatsApp")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingSale = null }) { Text(strings.close) }
            }
        )
    }

    confirmDeleteSale?.let { sale ->
        AlertDialog(
            onDismissRequest = { confirmDeleteSale = null },
            title = { Text(strings.delete) },
            text = {
                Text(
                    if (isBn) "${sale.invoiceNumber} মুছে ফেললে স্টক, ক্যাশবুক ও বাকি খাতা স্বয়ংক্রিয়ভাবে পুনরায় হিসাব হবে।"
                    else "Deleting ${sale.invoiceNumber} will automatically recalculate stock, cashbook, and due ledger."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSale(sale.id)
                    confirmDeleteSale = null
                }) {
                    Text(strings.delete, color = LossRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSale = null }) { Text(strings.cancel) }
            }
        )
    }
}
