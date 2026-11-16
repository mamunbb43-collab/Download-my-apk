package com.example.utils

import android.content.Context
import java.io.File

object CsvExporter {
    fun generateCsvFile(
        context: Context,
        headers: List<String>,
        rows: List<List<String>>,
        fileNamePrefix: String
    ): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        // UTF-8 BOM for proper Bengali rendering in Microsoft Excel
        sb.append('\uFEFF')
        sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\n", " ").replace("\"", "\"\"")
        return "\"$clean\""
    }
}
