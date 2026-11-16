package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.ShopSettings
import com.example.data.repository.DashboardMetrics
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.localization.LocalDatePattern
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.*
import com.example.utils.ShareAndMediaHelper

private data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    settings: ShopSettings,
    metrics: DashboardMetrics,
    onNavigateToSales: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToCashbook: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToDueBook: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSmsReminder: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToFinancialOverview: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val datePattern = LocalDatePattern.current
    val isBn = strings.isBengali
    val isOnline = remember { ShareAndMediaHelper.isNetworkOnline(context) }
    var showSupportDialog by remember { mutableStateOf(false) }

    val quickActions = remember(strings) {
        listOf(
            QuickActionItem(strings.qaSellProduct, Icons.Default.ShoppingCart, TileBlue, "qa_sell", onNavigateToSales),
            QuickActionItem(strings.qaProductList, Icons.AutoMirrored.Filled.FormatListBulleted, TileMagenta, "qa_products", onNavigateToProducts),
            QuickActionItem(strings.qaPurchaseProduct, Icons.Default.AddShoppingCart, TileOrange, "qa_purchase", onNavigateToPurchase),
            QuickActionItem(strings.qaCashBox, Icons.Default.Calculate, TileGreen, "qa_cashbox", onNavigateToCashbook),
            QuickActionItem(strings.qaShopExpenses, Icons.Default.MoneyOff, TileRed, "qa_expenses", onNavigateToExpenses),
            QuickActionItem(strings.qaDueBook, Icons.Default.Bookmark, TilePurple, "qa_duebook", onNavigateToDueBook),
            QuickActionItem(strings.qaSalesReport, Icons.Default.BarChart, TileAmber, "qa_reports", onNavigateToReports),
            QuickActionItem(strings.qaSmsReminder, Icons.Default.Sms, TilePink, "qa_sms", onNavigateToSmsReminder),
            QuickActionItem(strings.qaDataBackup, Icons.Default.CloudUpload, TileCyan, "qa_backup", onNavigateToBackup)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Brand Header matching reference screenshot
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = settings.shopName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .clickable(onClick = onNavigateToFinancialOverview)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("basic_overview_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.basicBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = strings.businessFinancialOverview,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showSupportDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("support_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = strings.contactSupport,
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.contactSupport,
                        style = MaterialTheme.typography.labelLarge,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Teal Green Hero Summary Card matching screenshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TealPrimary, TealPrimaryDark)
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = strings.date,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Formatters.formatDate(System.currentTimeMillis(), datePattern, isBn),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) MintOnline else AmberAccent)
                            )
                            Text(
                                text = if (isOnline) strings.online else strings.offlineReady,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.22f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.todaySalesCountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = Formatters.formatCount(metrics.todaySalesCount, isBn, "টি", "pcs"),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = strings.todayTotalAmountLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = Formatters.formatMoney(metrics.todayTotalSales, currency, isBn),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Actions 3x3 Colorful Grid matching screenshot
            Text(
                text = strings.quickActions,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                quickActions.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            QuickActionTile(
                                item = item,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Low Stock Alert Banner if applicable
            if (metrics.lowStockCount > 0) {
                Card(
                    onClick = onNavigateToStock,
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth().testTag("low_stock_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningAmber)
                        Text(
                            text = "${Formatters.toLocalizedDigits(metrics.lowStockCount.toString(), isBn)} ${strings.lowStockAlertBanner}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarningAmber)
                    }
                }
            }

            // Real-time 11 Dashboard Metrics
            Text(
                text = strings.todaySummary,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            val kpiList = listOf(
                Triple(strings.todayTotalSales, Formatters.formatMoney(metrics.todayTotalSales, currency, isBn), Pair(Icons.Default.PointOfSale, TileBlue)),
                Triple(strings.todayTotalPurchase, Formatters.formatMoney(metrics.todayTotalPurchase, currency, isBn), Pair(Icons.Default.Inventory, TileOrange)),
                Triple(strings.todayNetProfit, Formatters.formatMoney(metrics.todayNetProfit, currency, isBn), Pair(Icons.Default.TrendingUp, if (metrics.todayNetProfit >= 0) ProfitGreen else LossRed)),
                Triple(strings.todayTotalExpense, Formatters.formatMoney(metrics.todayTotalExpense, currency, isBn), Pair(Icons.Default.ReceiptLong, TileRed)),
                Triple(strings.totalCash, Formatters.formatMoney(metrics.totalCash, currency, isBn), Pair(Icons.Default.AccountBalanceWallet, TileGreen)),
                Triple(strings.bankAndWallet, Formatters.formatMoney(metrics.totalBankAndWallet, currency, isBn), Pair(Icons.Default.AccountBalance, TileCyan)),
                Triple(strings.customerReceivable, Formatters.formatMoney(metrics.totalCustomerReceivable, currency, isBn), Pair(Icons.Default.CallReceived, TilePurple)),
                Triple(strings.supplierPayable, Formatters.formatMoney(metrics.totalSupplierPayable, currency, isBn), Pair(Icons.Default.CallMade, TileMagenta)),
                Triple(strings.totalProducts, Formatters.formatCount(metrics.totalProductCount, isBn, "টি", ""), Pair(Icons.Default.Category, TealPrimary)),
                Triple(strings.lowStockProducts, Formatters.formatCount(metrics.lowStockCount, isBn, "টি", ""), Pair(Icons.Default.ProductionQuantityLimits, WarningAmber)),
                Triple(strings.totalInventoryValue, Formatters.formatMoney(metrics.totalInventoryValue, currency, isBn), Pair(Icons.Default.Store, InfoBlue))
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                kpiList.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { (label, valText, iconColor) ->
                            MetricStatCard(
                                title = label,
                                value = valText,
                                icon = iconColor.first,
                                accentColor = iconColor.second,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Business Financial Overview CTA Card
            Card(
                onClick = onNavigateToFinancialOverview,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth().testTag("financial_overview_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = strings.businessFinancialOverview,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.viewFullOverview,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text(settings.shopName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (settings.ownerName.isNotBlank()) {
                        Text("${strings.ownerName}: ${settings.ownerName}")
                    }
                    if (settings.shopPhone.isNotBlank()) {
                        Text("${strings.phone}: ${settings.shopPhone}")
                    }
                    if (settings.shopAddress.isNotBlank()) {
                        Text("${strings.address}: ${settings.shopAddress}")
                    }
                    HorizontalDivider()
                    Text(
                        text = if (isBn) "সব তথ্য আপনার ফোনে অফলাইনে সুরক্ষিত থাকে। নিয়মিত ডাটা ব্যাকআপ নিয়ে JSON ফাইল সংরক্ষণ করুন।"
                        else "All your business records are stored safely offline in SQLite. Export JSON backups regularly from Data Backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun QuickActionTile(
    item: QuickActionItem,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = item.onClick,
        shape = RoundedCornerShape(18.dp),
        color = item.color,
        shadowElevation = 3.dp,
        modifier = modifier
            .aspectRatio(1.02f)
            .testTag(item.tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
