package be.mygod.vpnhotspot.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import be.mygod.vpnhotspot.App.Companion.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@Database(entities = [ClientRecord::class, TrafficRecord::class], version = 3)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "app.db"

        val instance by lazy {
            Room.databaseBuilder(app.deviceStorage, AppDatabase::class.java, DB_NAME).apply {
                addMigrations(
                        Migration2, Migration3
                )
                setQueryExecutor(Dispatchers.IO.asExecutor())
            }.build()
        }
    }

    abstract val clientRecordDao: ClientRecord.Dao
    abstract val trafficRecordDao: TrafficRecord.Dao

    object Migration2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) =
            db.execSQL("ALTER TABLE `ClientRecord` ADD COLUMN `macLookupPending` INTEGER NOT NULL DEFAULT 1")
    }

    object Migration3 : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) =
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_TrafficRecord_mac` ON `TrafficRecord` (`mac`)")
    }
}
