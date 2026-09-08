package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScanRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scan_records WHERE rawCode = :rawCode ORDER BY timestamp DESC LIMIT 1")
    suspend fun findFirstByRawCode(rawCode: String): ScanRecord?

    @Query("SELECT COUNT(*) FROM scan_records WHERE rawCode = :rawCode")
    suspend fun countOccurrences(rawCode: String): Int

    @Query("SELECT * FROM scan_records WHERE id = :id")
    suspend fun getById(id: Long): ScanRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScanRecord): Long

    @Update
    suspend fun update(record: ScanRecord)

    @Delete
    suspend fun delete(record: ScanRecord)

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM scan_records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records WHERE isDuplicate = 1")
    fun getDuplicateCount(): Flow<Int>

    @Query("SELECT DISTINCT courierName FROM scan_records WHERE courierName != '' ORDER BY courierName ASC")
    fun getAllCouriers(): Flow<List<String>>
}
