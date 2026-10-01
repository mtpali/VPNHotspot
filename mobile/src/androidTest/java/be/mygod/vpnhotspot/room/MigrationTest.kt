package be.mygod.vpnhotspot.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    companion object {
        private const val TEST_DB = "migration-test"
    }

    @get:Rule
    val privateDatabase = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName, FrameworkSQLiteOpenHelperFactory())

    @Test
    @Throws(IOException::class)
    fun migrate2() {
        val db = privateDatabase.createDatabase(TEST_DB, 1)
        db.close()
        privateDatabase.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.Migration2)
    }

    @Test
    fun migrate3PreservesTraffic() {
        privateDatabase.createDatabase(TEST_DB, 2).apply {
            execSQL("""INSERT INTO TrafficRecord
                (id, timestamp, mac, ip, upstream, downstream, sentPackets, sentBytes, receivedPackets,
                    receivedBytes, previousId)
                VALUES (1, 1000, 187723572702975, X'C0A82B02', NULL, 'wlan0', 2, 100, 3, 200, NULL)""")
            close()
        }
        privateDatabase.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.Migration3).apply {
            query("SELECT sentBytes, receivedBytes FROM TrafficRecord WHERE id = 1").use { cursor ->
                org.junit.Assert.assertTrue(cursor.moveToFirst())
                org.junit.Assert.assertEquals(100L, cursor.getLong(0))
                org.junit.Assert.assertEquals(200L, cursor.getLong(1))
            }
            close()
        }
    }
}
