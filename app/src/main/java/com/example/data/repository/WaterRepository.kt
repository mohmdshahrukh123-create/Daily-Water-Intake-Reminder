package com.example.data.repository

import com.example.data.local.WaterLogDao
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterLogDao: WaterLogDao) {
    val allLogs: Flow<List<WaterLog>> = waterLogDao.getAllLogs()

    suspend fun insert(log: WaterLog) {
        waterLogDao.insertLog(log)
    }

    suspend fun deleteById(id: Int) {
        waterLogDao.deleteLogById(id)
    }

    suspend fun clearAll() {
        waterLogDao.clearAllLogs()
    }
}
