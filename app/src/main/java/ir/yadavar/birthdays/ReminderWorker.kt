package ir.yadavar.birthdays

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.yadavar.birthdays.data.AppDatabase
import java.util.Calendar

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val date = JalaliDate.fromGregorian(tomorrow)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "birthdays.db").build()
        val people = db.birthdayDao().birthdaysOn(date.month, date.day)
        db.close()
        if (people.isEmpty()) return Result.success()
        val names = people.joinToString("، ") { it.name }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "یادآوری تولد", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "یادآوری تولدها، یک روز پیش از موعد"
        })
        manager.notify(date.month * 100 + date.day, NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تولد فردا")
            .setContentText("فردا تولد $names است — ${date.display()}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("فردا تولد $names است — ${date.display()}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build())
        return Result.success()
    }
    companion object { const val CHANNEL_ID = "birthday_reminders" }
}
