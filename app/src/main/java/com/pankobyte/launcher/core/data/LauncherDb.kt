package com.pankobyte.launcher.core.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface KidDao {
    @Query("SELECT * FROM kid_profile ORDER BY id LIMIT 1")
    suspend fun firstProfile(): KidProfile?

    @Query("SELECT * FROM kid_profile WHERE id = :id")
    suspend fun profile(id: Long): KidProfile?

    @Query("SELECT * FROM kid_profile WHERE id = :id")
    fun observeProfile(id: Long): Flow<KidProfile?>

    @Insert
    suspend fun insertProfile(profile: KidProfile): Long

    @Update
    suspend fun updateProfile(profile: KidProfile)

    @Query("SELECT * FROM kid_app WHERE profileId = :profileId")
    fun observeApps(profileId: Long): Flow<List<KidApp>>

    @Query("SELECT * FROM kid_app WHERE profileId = :profileId")
    suspend fun apps(profileId: Long): List<KidApp>

    @Query("DELETE FROM kid_app WHERE profileId = :profileId")
    suspend fun clearApps(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<KidApp>)

    @Transaction
    suspend fun setApps(profileId: Long, packages: Collection<String>) {
        clearApps(profileId)
        insertApps(packages.map { KidApp(profileId, it) })
    }

    @Insert
    suspend fun insertSession(session: KidSession): Long

    @Query("UPDATE kid_session SET endedAt = :endedAt WHERE id = :id")
    suspend fun endSession(id: Long, endedAt: Long)
}

@Database(
    entities = [
        KidProfile::class, KidApp::class, KidContact::class, KidSession::class, KidSessionApp::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LauncherDb : RoomDatabase() {
    abstract fun kidDao(): KidDao

    companion object {
        /** Version 2 drops the tables of the removed Normal and Focus home screens. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS home_item")
                db.execSQL("DROP TABLE IF EXISTS focus_favourite")
                db.execSQL("DROP TABLE IF EXISTS app_flag")
            }
        }

        /** Version 3 replaces the tiles (Drawing, Stories and so on) with one flat list of allowed apps. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kid_app` (`profileId` INTEGER NOT NULL, `packageName` TEXT NOT NULL, " +
                        "PRIMARY KEY(`profileId`, `packageName`))",
                )
                // Keep what the parent had already allowed: apps of tiles that were switched on.
                db.execSQL(
                    "INSERT OR IGNORE INTO kid_app (profileId, packageName) " +
                        "SELECT t.profileId, a.packageName FROM kid_tile_app a JOIN kid_tile t ON t.id = a.tileId WHERE t.enabled = 1",
                )
                db.execSQL("DROP TABLE IF EXISTS kid_tile_app")
                db.execSQL("DROP TABLE IF EXISTS kid_tile")
            }
        }

        fun create(context: Context): LauncherDb =
            Room.databaseBuilder(context, LauncherDb::class.java, "launcher.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
