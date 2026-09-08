package com.example.data.repository

import com.example.data.db.ScanDao
import com.example.data.model.ScanRecord
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {

    val allScans: Flow<List<ScanRecord>> = scanDao.getAllScans()
    val totalCount: Flow<Int> = scanDao.getTotalCount()
    val duplicateCount: Flow<Int> = scanDao.getDuplicateCount()
    val allCouriers: Flow<List<String>> = scanDao.getAllCouriers()

    suspend fun findFirstByRawCode(rawCode: String): ScanRecord? {
        return scanDao.findFirstByRawCode(rawCode.trim())
    }

    suspend fun countOccurrences(rawCode: String): Int {
        return scanDao.countOccurrences(rawCode.trim())
    }

    suspend fun insertScan(record: ScanRecord): Long {
        return scanDao.insert(record)
    }

    suspend fun updateScan(record: ScanRecord) {
        scanDao.update(record)
    }

    suspend fun deleteScan(record: ScanRecord) {
        scanDao.delete(record)
    }

    suspend fun deleteById(id: Long) {
        scanDao.deleteById(id)
    }

    suspend fun deleteAll() {
        scanDao.deleteAll()
    }
}
