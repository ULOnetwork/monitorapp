package eu.ulonetwork.monitorapp.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordRuleDao {

    @Query("SELECT * FROM keyword_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<KeywordRule>>

    @Query("SELECT * FROM keyword_rules WHERE enabled = 1")
    suspend fun getEnabledRules(): List<KeywordRule>

    @Query("SELECT * FROM keyword_rules WHERE id = :id")
    suspend fun getById(id: Long): KeywordRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: KeywordRule): Long

    @Update
    suspend fun update(rule: KeywordRule)

    @Delete
    suspend fun delete(rule: KeywordRule)

    @Query("UPDATE keyword_rules SET lastTriggeredAt = :timestamp WHERE id = :ruleId")
    suspend fun updateLastTriggeredAt(ruleId: Long, timestamp: Long)
}
