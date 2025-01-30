package com.hari.management.util

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

object Scheduler {
    fun scheduleDailyExport(context: Context) {
        val currentDate = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0) // 12 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // If current time is past midnight, schedule for next day
            if (before(currentDate)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = scheduledTime.timeInMillis - currentDate.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<EmailExportWorker>(
            24, TimeUnit.HOURS,
            0, TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyEmailExport",
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }

    fun cancelDailyExport(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("DailyEmailExport")
    }
} 