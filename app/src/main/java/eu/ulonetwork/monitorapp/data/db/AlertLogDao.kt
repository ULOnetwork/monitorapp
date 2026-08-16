package eu.ulonetwork.monitorapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {

    @Query("SELECT * FROM alert_log_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AlertLogEntry>>

    @Insert
    suspend fun insert(entry: AlertLogEntry): Long
}
