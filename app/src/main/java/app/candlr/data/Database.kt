package app.candlr.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY name COLLATE NOCASE")
    fun observe(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays ORDER BY name COLLATE NOCASE") suspend fun all(): List<Birthday>

    @Upsert suspend fun put(birthday: Birthday)

    @Upsert suspend fun putAll(birthdays: List<Birthday>)

    @Query("DELETE FROM birthdays WHERE id = :id") suspend fun delete(id: String)

    @Query("DELETE FROM birthdays") suspend fun clear()

    @Query("SELECT * FROM preferences WHERE id = 1") fun observePreferences(): Flow<Preferences?>

    @Query("SELECT * FROM preferences WHERE id = 1") suspend fun preferences(): Preferences?

    @Upsert suspend fun putPreferences(preferences: Preferences)

    @Query("SELECT COUNT(*) FROM deliveries WHERE `key` = :key")
    suspend fun delivered(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mark(delivery: Delivery)

    @Query("DELETE FROM deliveries WHERE deliveredDate < :before")
    suspend fun pruneDeliveries(before: String)
}

@Database(
    entities = [Birthday::class, Preferences::class, Delivery::class],
    version = 1,
    exportSchema = true,
)
abstract class CandlrDatabase : RoomDatabase() {
    abstract fun birthdays(): BirthdayDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, CandlrDatabase::class.java, "candlr.db").build()
    }
}
