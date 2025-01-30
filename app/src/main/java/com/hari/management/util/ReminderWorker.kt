package com.hari.management.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.hari.management.R
import com.hari.management.data.GuestDatabase
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val guestId = inputData.getInt("guestId", -1)
        val guestName = inputData.getString("guestName") ?: return Result.failure()

        // Show notification
        showNotification(guestId, guestName)

        return Result.success()
    }

    private fun showNotification(guestId: Int, guestName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guest Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for guest follow-ups"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Guest Reminder")
            .setContentText("Follow up with $guestName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(guestId, notification)
    }

    companion object {
        private const val CHANNEL_ID = "guest_reminders"

        fun scheduleReminder(
            context: Context,
            guestId: Int,
            guestName: String,
            reminderTime: Long
        ) {
            val data = workDataOf(
                "guestId" to guestId,
                "guestName" to guestName
            )

            val reminderWork = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(data)
                .setInitialDelay(
                    reminderTime - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "reminder_$guestId",
                    ExistingWorkPolicy.REPLACE,
                    reminderWork
                )
        }

        fun cancelReminder(context: Context, guestId: Int) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("reminder_$guestId")
        }
    }
} 