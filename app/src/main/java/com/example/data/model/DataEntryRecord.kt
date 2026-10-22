package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "data_records")
data class DataEntryRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val certificate: String = "",
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val remaining: Double = 0.0,
    val payment: String = "Full Paid",
    val status: String = "Active",
    val mobile: String = "",
    val date: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
