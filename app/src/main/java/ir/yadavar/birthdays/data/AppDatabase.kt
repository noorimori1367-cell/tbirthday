package ir.yadavar.birthdays.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Birthday::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao
}
