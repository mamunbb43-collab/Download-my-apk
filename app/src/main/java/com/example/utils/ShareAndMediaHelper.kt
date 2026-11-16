package com.example.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.preferences.ShopSettings
import com.example.localization.Formatters
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ShareAndMediaHelper {

    fun copyPickedImageToInternalStorage(context: Context, uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "images").apply { mkdirs() }
            val dest = File(dir, "img_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        }.getOrNull()
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun openPdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(viewIntent)
        }.onFailure {
            shareFile(context, file, "application/pdf", "PDF")
        }
    }

    fun shareSaleOnWhatsApp(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        settings: ShopSettings
    ) {
        val isBn = settings.language.code == "BN"
        val cur = settings.currency
        val sb = StringBuilder()
        sb.append("*${settings.shopName}*\n")
        sb.append("${if (isBn) "ইনভয়েস" else "Invoice"}: ${sale.invoiceNumber}\n")
        sb.append("${if (isBn) "কাস্টমার" else "Customer"}: ${sale.customerName}\n")
        sb.append("--------------------\n")
        for (item in items) {
            sb.append("• ${item.productName} x${Formatters.formatQty(item.quantity, isBn)} = ${Formatters.formatMoney(item.lineTotal, cur, isBn)}\n")
        }
        sb.append("--------------------\n")
        sb.append("${if (isBn) "মোট বিল" else "Total"}: ${Formatters.formatMoney(sale.totalAmount, cur, isBn)}\n")
        sb.append("${if (isBn) "পরিশোধ" else "Paid"}: ${Formatters.formatMoney(sale.paidAmount, cur, isBn)}\n")
        if (sale.dueAmount > 0) {
            sb.append("${if (isBn) "বাকি" else "Due"}: ${Formatters.formatMoney(sale.dueAmount, cur, isBn)}\n")
        }
        sb.append(if (isBn) "\nধন্যবাদ!" else "\nThank you!")

        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            setPackage("com.whatsapp")
        }
        runCatching {
            context.startActivity(waIntent)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            context.startActivity(Intent.createChooser(fallback, if (isBn) "ইনভয়েস শেয়ার করুন" else "Share Invoice"))
        }
    }

    fun sendSmsDueReminder(
        context: Context,
        phone: String,
        customerName: String,
        dueAmount: Double,
        settings: ShopSettings
    ) {
        val isBn = settings.language.code == "BN"
        val amtText = Formatters.formatMoney(dueAmount, settings.currency, isBn)
        val message = if (isBn) {
            "প্রিয় $customerName, ${settings.shopName}-এ আপনার $amtText টাকা বাকি রয়েছে। অনুগ্রহ করে সুবিধামতো পরিশোধ করুন। ধন্যবাদ।"
        } else {
            "Dear $customerName, you have a pending due of $amtText at ${settings.shopName}. Please settle at your convenience. Thank you."
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, if (isBn) "রিমাইন্ডার পাঠান" else "Send Reminder"))
        }
    }

    fun isNetworkOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun notifyLowStock(context: Context, products: List<ProductEntity>, isBengali: Boolean) {
        if (products.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "low_stock_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (isBengali) "কম স্টকের সতর্কতা" else "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val names = products.take(3).joinToString(", ") { it.name }
        val title = if (isBengali) "কম স্টকের সতর্কতা (${products.size})" else "Low Stock Alert (${products.size})"
        val body = if (isBengali) "স্টক কমে গেছে: $names" else "Low stock: $names"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        manager.notify(2001, notification)
    }
}
