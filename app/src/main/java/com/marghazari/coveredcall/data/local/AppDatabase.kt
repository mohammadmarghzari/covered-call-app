package com.marghazari.coveredcall.data.local

import android.content.Context
import androidx.room.*

@Entity(tableName = "activity_records")
data class ActivityRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUid: String,
    val type: String,
    val symbol: String,
    val detailsJson: String,
    val timestampMillis: Long
)

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(record: ActivityRecordEntity)

    @Query("SELECT * FROM activity_records WHERE ownerUid = :uid ORDER BY timestampMillis DESC LIMIT 200")
    suspend fun getRecentForUser(uid: String): List<ActivityRecordEntity>

    @Query("DELETE FROM activity_records WHERE ownerUid = :uid")
    suspend fun clearForUser(uid: String)
}

@Database(entities = [ActivityRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coveredcall.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
