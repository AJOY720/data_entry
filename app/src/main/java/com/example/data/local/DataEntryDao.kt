package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DataEntryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DataEntryDao {
    @Query("SELECT * FROM data_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<DataEntryRecord>>

    @Query("SELECT * FROM data_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): DataEntryRecord?

    @Query("SELECT * FROM data_records WHERE LOWER(TRIM(certificate)) = LOWER(TRIM(:certificate)) LIMIT 1")
    suspend fun getRecordByCertificate(certificate: String): DataEntryRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DataEntryRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DataEntryRecord>)

    @Update
    suspend fun updateRecord(record: DataEntryRecord)

    @Delete
    suspend fun deleteRecord(record: DataEntryRecord)

    @Query("DELETE FROM data_records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    @Query("DELETE FROM data_records")
    suspend fun deleteAllRecords()
}
