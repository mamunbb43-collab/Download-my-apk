package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ProductEntity
import com.example.data.local.StockMovementEntity
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.*
import com.example.utils.ShareAndMediaHelper
import java.io.File

@Composable
fun ProductsAndStockScreen(
    products: List<ProductEntity>,
    stockMovements: List<StockMovementEntity>,
    initialTab: Int = 0,
    onSaveProduct: (ProductEntity, Boolean) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onAdjustStock: (productId: String, movementType: String, quantityDelta: Double, note: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val isBn = strings.isBengali

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var showLowStockOnly by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Pair<ProductEntity?, Boolean>?>(null) }
    var prefilledNewBarcode by remember { mutableStateOf<String?>(null) }
    var unmatchedScannedCode by remember { mutableStateOf<String?>(null) }
    var adjustTargetProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showSearchScanner by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery, showLowStockOnly) {
        products.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, true) ||
                p.code.contains(searchQuery, true) ||
                p.barcode.contains(searchQuery, true) ||
                p.category.contains(searchQuery, true)
            val matchLow = !showLowStockOnly || p.currentStock <= p.minStock
            matchQuery && matchLow
        }
    }

    Scaffold(
        topBar = {
            AmarDokanTopBar(
                title = if (selectedTab == 0) strings.qaProductList else strings.stockManagement,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showSearchScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { editingProduct = null to true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(strings.addProduct) },
                    containerColor = TealPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
                )
            }
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
                    text = { Text("${strings.qaProductList} (${Formatters.toLocalizedDigits(products.size.toString(), isBn)})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(strings.stockManagement) }
                )
            }

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                IconButton(onClick = { showSearchScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_search_input")
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !showLowStockOnly,
                            onClick = { showLowStockOnly = false },
                            label = { Text(strings.all) }
                        )
                        val lowCount = products.count { it.currentStock <= it.minStock }
                        FilterChip(
                            selected = showLowStockOnly,
                            onClick = { showLowStockOnly = true },
                            label = { Text("${strings.lowStock} (${Formatters.toLocalizedDigits(lowCount.toString(), isBn)})") }
                        )
                        AssistChip(
                            onClick = { showSearchScanner = true },
                            leadingIcon = {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                            },
                            label = { Text(strings.scanBarcode) },
                            modifier = Modifier.testTag("product_list_scan_barcode_button")
                        )
                    }
                }

                if (filteredProducts.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Inventory,
                        message = strings.noData,
                        actionLabel = strings.addProduct,
                        onAction = { editingProduct = null to true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            ProductCard(
                                product = prod,
                                isBn = isBn,
                                onEdit = { editingProduct = prod to false },
                                onAdjustStock = { adjustTargetProduct = prod },
                                onDelete = { confirmDelete = prod }
                            )
                        }
                    }
                }
            } else {
                // Stock Management Tab
                val totalCostVal = products.sumOf { (it.currentStock * it.purchasePrice).coerceAtLeast(0.0) }
                val totalPotentialSaleVal = products.sumOf { (it.currentStock * it.sellingPrice).coerceAtLeast(0.0) }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricStatCard(
                                title = strings.totalInventoryValue,
                                value = Formatters.formatMoney(totalCostVal, currency, isBn),
                                icon = Icons.Default.Inventory2,
                                accentColor = TealPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = strings.potentialSaleValue,
                                value = Formatters.formatMoney(totalPotentialSaleVal, currency, isBn),
                                icon = Icons.Default.Sell,
                                accentColor = ProfitGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = if (isBn) "পণ্যভিত্তিক স্টক রিপোর্ট ও অ্যাডজাস্টমেন্ট" else "Product Stock Report & Adjustments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(products, key = { "stock_${it.id}" }) { prod ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
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
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    val statusColor = when {
                                        prod.currentStock <= 0 -> LossRed
                                        prod.currentStock <= prod.minStock -> WarningAmber
                                        else -> ProfitGreen
                                    }
                                    Text(
                                        text = "${Formatters.formatQty(prod.currentStock, isBn)} ${prod.unit}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isBn) {
                                        "ক্রয়: ${Formatters.formatQty(prod.totalPurchasedQty, true)} | বিক্রয়: ${Formatters.formatQty(prod.totalSoldQty, true)} | নষ্ট: ${Formatters.formatQty(prod.totalDamagedQty, true)} | ফেরত: ${Formatters.formatQty(prod.totalReturnedQty, true)}"
                                    } else {
                                        "Bought: ${Formatters.formatQty(prod.totalPurchasedQty, false)} | Sold: ${Formatters.formatQty(prod.totalSoldQty, false)} | Damaged: ${Formatters.formatQty(prod.totalDamagedQty, false)} | Return: ${Formatters.formatQty(prod.totalReturnedQty, false)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { adjustTargetProduct = prod }) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.stockAdjustment)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = strings.stockHistory,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (stockMovements.isEmpty()) {
                        item { Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(stockMovements.take(40), key = { it.id }) { mov ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${mov.productName} (${mov.movementType})", style = MaterialTheme.typography.titleSmall)
                                        if (mov.note.isNotBlank()) {
                                            Text(mov.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val prefix = if (mov.quantityDelta >= 0) "+" else ""
                                        Text(
                                            text = "$prefix${Formatters.formatQty(mov.quantityDelta, isBn)}",
                                            color = if (mov.quantityDelta >= 0) ProfitGreen else LossRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${strings.currentStock}: ${Formatters.formatQty(mov.stockAfter, isBn)}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSearchScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { rawCode ->
                val code = rawCode.trim()
                showSearchScanner = false
                val match = products.find {
                    it.barcode.equals(code, ignoreCase = true) || it.code.equals(code, ignoreCase = true)
                }
                if (match != null) {
                    searchQuery = code
                } else {
                    unmatchedScannedCode = code
                }
            },
            onDismiss = { showSearchScanner = false }
        )
    }

    unmatchedScannedCode?.let { scannedCode ->
        AlertDialog(
            onDismissRequest = { unmatchedScannedCode = null },
            title = { Text(strings.scanBarcode) },
            text = {
                Text(
                    if (isBn) {
                        "বারকোড '$scannedCode' এর কোনো পণ্য পাওয়া যায়নি। এই বারকোড দিয়ে নতুন পণ্য যোগ করবেন?"
                    } else {
                        "No product found with barcode '$scannedCode'. Would you like to add a new product with this barcode?"
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    prefilledNewBarcode = scannedCode
                    unmatchedScannedCode = null
                    editingProduct = null to true
                }) {
                    Text(strings.addProduct)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    searchQuery = scannedCode
                    unmatchedScannedCode = null
                }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    editingProduct?.let { (product, isNew) ->
        ProductEditorDialog(
            existing = product,
            isNew = isNew,
            initialBarcode = prefilledNewBarcode ?: "",
            onDismiss = {
                prefilledNewBarcode = null
                editingProduct = null
            },
            onSave = { saved ->
                onSaveProduct(saved, isNew)
                prefilledNewBarcode = null
                editingProduct = null
            }
        )
    }

    adjustTargetProduct?.let { prod ->
        StockAdjustmentDialog(
            product = prod,
            onDismiss = { adjustTargetProduct = null },
            onConfirm = { type, delta, note ->
                onAdjustStock(prod.id, type, delta, note)
                adjustTargetProduct = null
            }
        )
    }

    confirmDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(strings.delete) },
            text = { Text("${prod.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteProduct(prod)
                    confirmDelete = null
                }) {
                    Text(strings.delete, color = LossRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    isBn: Boolean,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val stockColor = when {
        product.currentStock <= 0 -> LossRed
        product.currentStock <= product.minStock -> WarningAmber
        else -> ProfitGreen
    }
    val stockBadge = when {
        product.currentStock <= 0 -> strings.outOfStock
        product.currentStock <= product.minStock -> strings.lowStock
        else -> "${Formatters.formatQty(product.currentStock, isBn)} ${product.unit}"
    }

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (product.imageUri.isNotBlank() && File(product.imageUri).exists()) {
                AsyncImage(
                    model = File(product.imageUri),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = TealPrimary)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(stockColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(stockBadge, style = MaterialTheme.typography.labelSmall, color = stockColor)
                    }
                }
                Text(
                    text = "${strings.purchasePrice}: ${Formatters.formatMoney(product.purchasePrice, currency, isBn)} | ${strings.sellingPrice}: ${Formatters.formatMoney(product.sellingPrice, currency, isBn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listOfNotNull(
                            product.category.takeIf { it.isNotBlank() },
                            product.barcode.takeIf { it.isNotBlank() }
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        IconButton(onClick = onAdjustStock, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = strings.stockAdjustment, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = strings.delete, tint = LossRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    existing: ProductEntity?,
    isNew: Boolean,
    initialBarcode: String = "",
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var code by remember { mutableStateOf(existing?.code ?: "") }
    var barcode by remember(existing, initialBarcode) {
        mutableStateOf(existing?.barcode?.takeIf { it.isNotBlank() } ?: initialBarcode)
    }
    var category by remember { mutableStateOf(existing?.category ?: if (strings.isBengali) "সাধারণ" else "General") }
    var purchasePrice by remember { mutableStateOf(existing?.purchasePrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(existing?.sellingPrice?.toString() ?: "") }
    var currentStock by remember { mutableStateOf(existing?.currentStock?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(existing?.minStock?.toString() ?: "5") }
    var unit by remember { mutableStateOf(existing?.unit ?: if (strings.isBengali) "পিস" else "pcs") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var imageUri by remember { mutableStateOf(existing?.imageUri ?: "") }
    var showScanner by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ShareAndMediaHelper.copyPickedImageToInternalStorage(context, uri)?.let { path ->
                imageUri = path
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) strings.addProduct else strings.editProduct) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (imageUri.isNotBlank() && File(imageUri).exists()) {
                        AsyncImage(
                            model = File(imageUri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickPhoto)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.productName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(strings.productCode) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text(strings.category) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(strings.barcode) },
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = strings.scanBarcode)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_barcode_input")
                )
                FilledTonalButton(
                    onClick = { showScanner = true },
                    modifier = Modifier.fillMaxWidth().testTag("product_editor_scan_barcode"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TealPrimary.copy(alpha = 0.14f),
                        contentColor = TealPrimary
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (strings.isBengali) "ক্যামেরা দিয়ে বারকোড স্ক্যান" else "Scan Barcode with Camera")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text(strings.purchasePrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text(strings.sellingPrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text(strings.currentStock) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = it },
                        label = { Text(strings.minStock) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(strings.unit) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(strings.description) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (existing ?: ProductEntity(id = "", name = name)).copy(
                        name = name.trim(),
                        code = code.trim(),
                        barcode = barcode.trim(),
                        category = category.trim().ifBlank { "সাধারণ" },
                        purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellingPrice.toDoubleOrNull() ?: 0.0,
                        currentStock = currentStock.toDoubleOrNull() ?: 0.0,
                        minStock = minStock.toDoubleOrNull() ?: 5.0,
                        unit = unit.trim().ifBlank { "পিস" },
                        description = description.trim(),
                        imageUri = imageUri
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )

    if (showScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showScanner = false
            },
            onDismiss = { showScanner = false }
        )
    }
}

@Composable
private fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (movementType: String, quantityDelta: Double, note: String) -> Unit
) {
    val strings = LocalAppStrings.current
    var type by remember { mutableStateOf("DAMAGE") }
    var qtyInput by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncrease by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.stockAdjustment} — ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DAMAGE" to strings.stockDamaged,
                        "SALE_RETURN" to strings.stockReturnSale,
                        "PURCHASE_RETURN" to strings.stockReturnPurchase,
                        "ADJUSTMENT" to strings.stockAdjustment
                    ).forEach { (k, label) ->
                        FilterChip(
                            selected = type == k,
                            onClick = {
                                type = k
                                isIncrease = (k == "SALE_RETURN")
                            },
                            label = { Text(label) }
                        )
                    }
                }
                if (type == "ADJUSTMENT") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isIncrease,
                            onClick = { isIncrease = true },
                            label = { Text(if (strings.isBengali) "+ যোগ করুন" else "+ Increase") }
                        )
                        FilterChip(
                            selected = !isIncrease,
                            onClick = { isIncrease = false },
                            label = { Text(if (strings.isBengali) "- কমান" else "- Decrease") }
                        )
                    }
                }
                OutlinedTextField(
                    value = qtyInput,
                    onValueChange = { qtyInput = it },
                    label = { Text(strings.quantity) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.note) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyInput.toDoubleOrNull() ?: 0.0
                    if (qty > 0) {
                        val delta = when (type) {
                            "SALE_RETURN" -> qty
                            "DAMAGE", "PURCHASE_RETURN" -> -qty
                            else -> if (isIncrease) qty else -qty
                        }
                        onConfirm(type, delta, note)
                    }
                },
                enabled = (qtyInput.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
