package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE timestamp >= :timestamp")
    suspend fun getWaterSumSince(timestamp: Long): Int?

    @Query("SELECT * FROM water_logs WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    fun getLogsSinceFlow(timestamp: Long): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM water_logs")
    suspend fun clearAllLogs()
}
