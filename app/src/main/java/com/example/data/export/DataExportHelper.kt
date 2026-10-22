package com.example.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.DataEntryRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object DataExportHelper {

    fun generateCsv(records: List<DataEntryRecord>): String {
        val builder = StringBuilder()
        // Add UTF-8 BOM so Microsoft Excel correctly renders Bengali and special characters
        builder.append("\uFEFF")
        
        // Header
        val headers = listOf(
            "Serial No",
            "Name",
            "Certificate No",
            "Total Amount",
            "Amount Paid",
            "Remaining",
            "Payment Type",
            "Status",
            "Mobile No / Reference No",
            "Date",
            "Description"
        )
        builder.append(headers.joinToString(",") { escapeCsv(it) }).append("\r\n")

        records.forEachIndexed { index, record ->
            val row = listOf(
                (index + 1).toString(),
                record.name,
                record.certificate,
                String.format(Locale.US, "%.2f", record.totalAmount),
                String.format(Locale.US, "%.2f", record.amountPaid),
                String.format(Locale.US, "%.2f", record.remaining),
                record.payment,
                record.status,
                record.mobile,
                record.date,
                record.description
            )
            builder.append(row.joinToString(",") { escapeCsv(it) }).append("\r\n")
        }

        return builder.toString()
    }

    fun generateBackupJson(records: List<DataEntryRecord>): String {
        val root = JSONObject()
        root.put("app", "Data Entry Register")
        root.put("version", 3)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
        
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject()
            obj.put("id", record.id)
            obj.put("name", record.name)
            obj.put("certificate", record.certificate)
            obj.put("totalAmount", record.totalAmount)
            obj.put("amountPaid", record.amountPaid)
            obj.put("remaining", record.remaining)
            obj.put("payment", record.payment)
            obj.put("status", record.status)
            obj.put("mobile", record.mobile)
            obj.put("date", record.date)
            obj.put("description", record.description)
            obj.put("createdAt", record.createdAt)
            obj.put("updatedAt", record.updatedAt)
            jsonArray.put(obj)
        }
        root.put("records", jsonArray)
        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): List<DataEntryRecord> {
        val records = mutableListOf<DataEntryRecord>()
        val trimmed = jsonString.trim()
        val jsonArray: JSONArray = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val root = JSONObject(trimmed)
            root.optJSONArray("records") ?: JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val total = obj.optDouble("totalAmount", 0.0)
            val paid = obj.optDouble("amountPaid", 0.0)
            val remaining = if (obj.has("remaining")) obj.optDouble("remaining", 0.0) else (total - paid).coerceAtLeast(0.0)
            
            var paymentType = obj.optString("payment", "Full Paid")
            if (paymentType == "Fully Paid") paymentType = "Full Paid"
            if (paymentType == "Partially Paid") paymentType = "Partial Paid"
            if (paymentType == "Total Due") paymentType = "Unpaid"

            records.add(
                DataEntryRecord(
                    id = obj.optString("id", UUID.randomUUID().toString()).ifEmpty { UUID.randomUUID().toString() },
                    name = obj.optString("name", "Unnamed"),
                    certificate = obj.optString("certificate", ""),
                    totalAmount = total,
                    amountPaid = paid,
                    remaining = remaining,
                    payment = paymentType,
                    status = obj.optString("status", "Active"),
                    mobile = obj.optString("mobile", ""),
                    date = obj.optString("date", ""),
                    description = obj.optString("description", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            )
        }
        return records
    }

    fun shareExportFile(context: Context, filename: String, content: String, mimeType: String) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, filename)
        file.writeText(content, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share or Save $filename"))
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
