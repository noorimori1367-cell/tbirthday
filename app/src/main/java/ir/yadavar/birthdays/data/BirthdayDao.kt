package ir.yadavar.birthdays.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY month, day, name")
    fun observeAll(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE month = :month AND day = :day")
    suspend fun birthdaysOn(month: Int, day: Int): List<Birthday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(birthday: Birthday)

    @androidx.room.Update
    suspend fun update(birthday: Birthday)

    @Delete
    suspend fun delete(birthday: Birthday)
}
