package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.FinancialOverview
import com.example.localization.Formatters
import com.example.localization.LocalAppStrings
import com.example.localization.LocalCurrency
import com.example.ui.components.AmarDokanTopBar
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.*

@Composable
fun FinancialOverviewScreen(
    overview: FinancialOverview,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current
    val isBn = strings.isBengali

    Scaffold(
        topBar = {
            AmarDokanTopBar(title = strings.businessFinancialOverview, onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(TealPrimary, TealPrimaryDark)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = strings.foNetAssets,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = Formatters.formatMoney(overview.netBusinessAssets, currency, isBn),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.foAllTimeProfitLoss}: ${Formatters.formatMoney(overview.allTimeNetProfit, currency, isBn)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (overview.allTimeNetProfit >= 0) Color(0xFFB9F6CA) else Color(0xFFFFCDD2)
                    )
                }
            }

            val items: List<Triple<String, Double, Pair<ImageVector, Color>>> = listOf(
                Triple(strings.foInventoryValue, overview.inventoryCostValue, Icons.Default.Inventory2 to TealPrimary),
                Triple(strings.foCashInHand, overview.cashInHand, Icons.Default.Payments to TileGreen),
                Triple(strings.foBankBalance, overview.bankAndWalletBalance, Icons.Default.AccountBalance to TileCyan),
                Triple(strings.foReceivable, overview.customerReceivables, Icons.Default.CallReceived to TilePurple),
                Triple(strings.foPayable, overview.supplierPayables, Icons.Default.CallMade to TileRed),
                Triple(strings.foTotalInvestment, overview.totalCapitalInvestment, Icons.Default.Savings to TileBlue),
                Triple(strings.foNetAssets, overview.netBusinessAssets, Icons.Default.AccountBalanceWallet to ProfitGreen),
                Triple(strings.foAllTimeProfitLoss, overview.allTimeNetProfit, Icons.Default.TrendingUp to if (overview.allTimeNetProfit >= 0) ProfitGreen else LossRed)
            )

            items.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (label, value, iconPair) ->
                        MetricStatCard(
                            title = label,
                            value = Formatters.formatMoney(value, currency, isBn),
                            icon = iconPair.first,
                            accentColor = iconPair.second,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
