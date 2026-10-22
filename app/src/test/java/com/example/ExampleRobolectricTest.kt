package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.DataExportHelper
import com.example.data.model.DataEntryRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Data Entry", appName)
  }

  @Test
  fun `csv export includes headers and formatted values`() {
    val records = listOf(
      DataEntryRecord(
        name = "Ajoy Mandal",
        certificate = "CERT-101",
        totalAmount = 5000.0,
        amountPaid = 3500.0,
        remaining = 1500.0,
        payment = "Partial Paid",
        status = "Active"
      )
    )
    val csv = DataExportHelper.generateCsv(records)
    assertTrue(csv.contains("CERT-101"))
    assertTrue(csv.contains("Ajoy Mandal"))
    assertTrue(csv.contains("3500.00"))
  }
}
