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
    version = 5,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keyword_rules ADD COLUMN issueActive INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN eventType TEXT NOT NULL DEFAULT 'ISSUE'")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN emailError TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keyword_rules ADD COLUMN screenGateKeyword TEXT")
            }
        }

        // hasBaseline defaults to 0 for existing rules too: on the next evaluation each rule just
        // silently records its current state as the baseline instead of alerting, rather than
        // risking an ISSUE/RESOLVED storm from every pre-existing rule right after this update.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keyword_rules ADD COLUMN hasBaseline INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE keyword_rules ADD COLUMN notifyTelegram INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN notifiedTelegram INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE alert_log_entries ADD COLUMN telegramError TEXT")
            }
        }
    }
}
