package com.example.data.repository

import com.example.data.local.DataEntryDao
import com.example.data.model.DataEntryRecord
import kotlinx.coroutines.flow.Flow

class DataEntryRepository(private val dao: DataEntryDao) {
    val allRecords: Flow<List<DataEntryRecord>> = dao.getAllRecords()

    suspend fun getRecordById(id: String): DataEntryRecord? = dao.getRecordById(id)

    suspend fun getRecordByCertificate(certificate: String): DataEntryRecord? {
        val trimmed = certificate.trim()
        if (trimmed.isEmpty()) return null
        return dao.getRecordByCertificate(trimmed)
    }

    suspend fun insert(record: DataEntryRecord) {
        dao.insertRecord(record)
    }

    suspend fun update(record: DataEntryRecord) {
        dao.updateRecord(record)
    }

    suspend fun deleteById(id: String) {
        dao.deleteRecordById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllRecords()
    }

    suspend fun restoreRecords(records: List<DataEntryRecord>) {
        dao.deleteAllRecords()
        dao.insertAll(records)
    }
}
