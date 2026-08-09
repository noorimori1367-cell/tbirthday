package ir.yadavar.birthdays.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "birthdays")
data class Birthday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val day: Int,
    val month: Int,
    val year: Int? = null
)
