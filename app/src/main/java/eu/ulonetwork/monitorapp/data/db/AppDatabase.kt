package eu.ulonetwork.monitorapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [KeywordRule::class, AlertLogEntry::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun alertLogDao(): AlertLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unetworkmonitor.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keyword_rules ADD COLUMN issueActive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN eventType TEXT NOT NULL DEFAULT 'ISSUE'")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN emailError TEXT")
            }
        }
    }
}
