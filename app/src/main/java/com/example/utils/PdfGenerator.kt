package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.SaleEntity
import com.example.data.local.SaleItemEntity
import com.example.data.preferences.ShopSettings
import com.example.localization.Formatters
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateSaleInvoicePdf(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        settings: ShopSettings
    ): File {
        val isBn = settings.language.code == "BN"
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBgPaint = Paint().apply { color = Color.parseColor("#00696E") }
        val whiteTitle = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whiteSub = Paint().apply {
            color = Color.parseColor("#E0F2F1")
            textSize = 11f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#141E22")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2A363B")
            textSize = 11.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D0DCE0")
            strokeWidth = 1f
        }
        val tableHeaderBg = Paint().apply { color = Color.parseColor("#EAF4F5") }

        // Teal Header Banner
        canvas.drawRect(0f, 0f, 595f, 110f, headerBgPaint)
        canvas.drawText(settings.shopName, 36f, 45f, whiteTitle)
        if (settings.shopAddress.isNotBlank()) {
            canvas.drawText(settings.shopAddress, 36f, 67f, whiteSub)
        }
        if (settings.shopPhone.isNotBlank()) {
            canvas.drawText("Phone: ${settings.shopPhone}", 36f, 85f, whiteSub)
        }

        val invTitle = if (isBn) "বিক্রয় ইনভয়েস / INVOICE" else "SALES INVOICE"
        canvas.drawText(invTitle, 380f, 45f, whiteTitle.apply { textSize = 16f })
        canvas.drawText(sale.invoiceNumber, 380f, 67f, whiteSub)
        canvas.drawText(
            Formatters.formatDate(sale.timestamp, settings.dateFormat.pattern, false),
            380f,
            85f,
            whiteSub
        )

        var y = 140f
        canvas.drawText(if (isBn) "কাস্টমার:" else "Customer:", 36f, y, boldPaint)
        canvas.drawText(sale.customerName, 115f, y, bodyPaint)
        if (sale.customerPhone.isNotBlank()) {
            y += 18f
            canvas.drawText(if (isBn) "মোবাইল:" else "Phone:", 36f, y, boldPaint)
            canvas.drawText(sale.customerPhone, 115f, y, bodyPaint)
        }
        canvas.drawText(
            "${if (isBn) "পেমেন্ট:" else "Payment:"} ${sale.paymentMethod}",
            380f,
            140f,
            bodyPaint
        )

        y += 28f
        // Table Header
        canvas.drawRect(36f, y, 559f, y + 26f, tableHeaderBg)
        canvas.drawText(if (isBn) "পণ্যের নাম" else "Item", 44f, y + 17f, boldPaint)
        canvas.drawText(if (isBn) "পরিমাণ" else "Qty", 300f, y + 17f, boldPaint)
        canvas.drawText(if (isBn) "দর" else "Rate", 385f, y + 17f, boldPaint)
        canvas.drawText(if (isBn) "মোট" else "Total", 480f, y + 17f, boldPaint)
        y += 38f

        for (item in items) {
            canvas.drawText(item.productName.take(34), 44f, y, bodyPaint)
            canvas.drawText(Formatters.formatQty(item.quantity, false), 300f, y, bodyPaint)
            canvas.drawText(Formatters.formatMoney(item.unitPrice, settings.currency, false), 385f, y, bodyPaint)
            canvas.drawText(Formatters.formatMoney(item.lineTotal, settings.currency, false), 480f, y, bodyPaint)
            y += 10f
            canvas.drawLine(36f, y, 559f, y, linePaint)
            y += 18f
            if (y > 720f) break
        }

        y += 12f
        val cur = settings.currency
        canvas.drawText(if (isBn) "সাবটোটাল:" else "Subtotal:", 360f, y, bodyPaint)
        canvas.drawText(Formatters.formatMoney(sale.subtotal, cur, false), 480f, y, bodyPaint)
        y += 18f
        canvas.drawText(if (isBn) "ডিসকাউন্ট:" else "Discount:", 360f, y, bodyPaint)
        canvas.drawText(Formatters.formatMoney(sale.discount, cur, false), 480f, y, bodyPaint)
        y += 20f
        canvas.drawLine(355f, y - 12f, 559f, y - 12f, linePaint)
        canvas.drawText(if (isBn) "সর্বমোট বিল:" else "Total Bill:", 360f, y, boldPaint)
        canvas.drawText(Formatters.formatMoney(sale.totalAmount, cur, false), 480f, y, boldPaint)
        y += 20f
        canvas.drawText(if (isBn) "পরিশোধিত:" else "Paid:", 360f, y, bodyPaint)
        canvas.drawText(Formatters.formatMoney(sale.paidAmount, cur, false), 480f, y, bodyPaint)
        y += 18f
        canvas.drawText(if (isBn) "বাকি:" else "Due:", 360f, y, boldPaint)
        canvas.drawText(Formatters.formatMoney(sale.dueAmount, cur, false), 480f, y, boldPaint)

        y = 800f
        canvas.drawLine(36f, y - 18f, 559f, y - 18f, linePaint)
        canvas.drawText(
            if (isBn) "ধন্যবাদ! আবার আসবেন — ${settings.shopName}" else "Thank you for your business — ${settings.shopName}",
            36f,
            y,
            bodyPaint
        )

        doc.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${sale.invoiceNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun generateTableReportPdf(
        context: Context,
        title: String,
        subtitle: String,
        summaryLines: List<Pair<String, String>>,
        headers: List<String>,
        rows: List<List<String>>,
        settings: ShopSettings,
        fileNamePrefix: String
    ): File {
        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
        var canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#00696E") }
        val whiteTitle = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whiteSub = Paint().apply {
            color = Color.parseColor("#E0F2F1")
            textSize = 11f
            isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.parseColor("#141E22")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#2A363B")
            textSize = 10.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#D5E0E3")
            strokeWidth = 1f
        }

        canvas.drawRect(0f, 0f, 595f, 88f, headerBg)
        canvas.drawText("${settings.shopName} — $title", 32f, 38f, whiteTitle)
        canvas.drawText(subtitle, 32f, 60f, whiteSub)

        var y = 112f
        for ((k, v) in summaryLines) {
            canvas.drawText(k, 32f, y, boldPaint)
            canvas.drawText(v, 300f, y, bodyPaint)
            y += 18f
        }

        y += 10f
        val colCount = headers.size.coerceAtLeast(1)
        val colWidth = 530f / colCount
        canvas.drawRect(32f, y, 563f, y + 22f, Paint().apply { color = Color.parseColor("#EAF4F5") })
        headers.forEachIndexed { idx, h ->
            canvas.drawText(h.take(18), 36f + idx * colWidth, y + 15f, boldPaint)
        }
        y += 32f

        for (row in rows) {
            if (y > 790f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
            row.forEachIndexed { idx, cell ->
                canvas.drawText(cell.take(20), 36f + idx * colWidth, y, bodyPaint)
            }
            y += 6f
            canvas.drawLine(32f, y, 563f, y, linePaint)
            y += 16f
        }

        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
