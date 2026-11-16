package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ExpenseEntity
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.LossRed
import com.example.ui.theme.TileRed
import com.example.utils.ShareAndMediaHelper
import java.io.File

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    onSaveExpense: (ExpenseEntity, Boolean) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali

    var selectedCategory by remember { mutableStateOf("ALL") }
    var editingExpense by remember { mutableStateOf<Pair<ExpenseEntity?, Boolean>?>(null) }

    val categories = remember(isBn) {
        listOf(
            "ALL" to strings.all,
            "RENT" to (if (isBn) "দোকান ভাড়া" else "Rent"),
            "ELECTRICITY" to (if (isBn) "বিদ্যুৎ বিল" else "Electricity"),
            "SALARY" to (if (isBn) "কর্মচারীর বেতন" else "Salary"),
            "TRANSPORT" to (if (isBn) "পরিবহন খরচ" else "Transport"),
            "FOOD" to (if (isBn) "খাবার খরচ" else "Food"),
            "OTHER" to (if (isBn) "অন্যান্য" else "Other")
        )
    }

    val filtered = remember(expenses, selectedCategory) {
        if (selectedCategory == "ALL") expenses else expenses.filter { it.category == selectedCategory }
    }
    val totalFiltered = filtered.sumOf { it.amount }

    Scaffold(
        topBar = {
            AmarDokanTopBar(title = strings.qaShopExpenses, onBack = onBack)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingExpense = null to true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.addExpense) },
                containerColor = TileRed,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TileRed)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(strings.qaShopExpenses, color = Color.White.copy(alpha = 0.85f))
                            Text(
                                Formatters.formatMoney(totalFiltered, currency, isBn),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(32.dp))
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    message = strings.noData,
                    actionLabel = strings.addExpense,
                    onAction = { editingExpense = null to true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { exp ->
                        Card(
                            onClick = { editingExpense = exp to false },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (exp.receiptUri.isNotBlank() && File(exp.receiptUri).exists()) {
                                    AsyncImage(
                                        model = File(exp.receiptUri),
                                        contentDescription = exp.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(exp.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${categories.find { it.first == exp.category }?.second ?: exp.category} • ${Formatters.formatDate(exp.timestamp, datePattern, isBn)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (exp.note.isNotBlank()) {
                                        Text(exp.note, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        Formatters.formatMoney(exp.amount, currency, isBn),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LossRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { onDeleteExpense(exp) }, modifier = Modifier.size(28.dp)) {
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

    editingExpense?.let { (existing, isNew) ->
        val context = LocalContext.current
        var title by remember { mutableStateOf(existing?.title ?: "") }
        var category by remember { mutableStateOf(existing?.category ?: "RENT") }
        var amountInput by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
        var method by remember { mutableStateOf(existing?.paymentMethod ?: "CASH") }
        var note by remember { mutableStateOf(existing?.note ?: "") }
        var receiptUri by remember { mutableStateOf(existing?.receiptUri ?: "") }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                ShareAndMediaHelper.copyPickedImageToInternalStorage(context, uri)?.let { receiptUri = it }
            }
        }

        AlertDialog(
            onDismissRequest = { editingExpense = null },
            title = { Text(strings.addExpense) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(strings.expenseTitle) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.drop(1).forEach { (k, l) ->
                            FilterChip(selected = category == k, onClick = { category = k }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH" to strings.cash, "BANK" to strings.bank, "MOBILE_WALLET" to strings.mobileWallet).forEach { (k, l) ->
                            FilterChip(selected = method == k, onClick = { method = k }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(strings.note) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.receiptPhoto)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amt > 0) {
                            onSaveExpense(
                                (existing ?: ExpenseEntity(id = "", title = title, category = category, amount = amt)).copy(
                                    title = title.trim(),
                                    category = category,
                                    amount = amt,
                                    paymentMethod = method,
                                    note = note.trim(),
                                    receiptUri = receiptUri
                                ),
                                isNew
                            )
                            editingExpense = null
                        }
                    },
                    enabled = title.isNotBlank() && (amountInput.toDoubleOrNull() ?: 0.0) > 0
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editingExpense = null }) { Text(strings.cancel) }
            }
        )
    }
}
