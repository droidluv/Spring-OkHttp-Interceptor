package com.readystatesoftware.chuck.internal.support

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.readystatesoftware.chuck.ChuckInterceptor
import com.readystatesoftware.chuck.internal.data.ChuckContentProvider
import java.util.*
import java.util.concurrent.TimeUnit

class RetentionManager(private val context: Context, retentionPeriod: ChuckInterceptor.Period) {
    private val period: Long
    private val cleanupFrequency: Long
    private val prefs: SharedPreferences

    init {
        period = toMillis(retentionPeriod)
        prefs = context.getSharedPreferences(PREFS_NAME, 0)
        cleanupFrequency = if (retentionPeriod === ChuckInterceptor.Period.ONE_HOUR)
            TimeUnit.MINUTES.toMillis(30)
        else
            TimeUnit.HOURS.toMillis(2)
    }

    @Synchronized
    fun doMaintenance() {
        if (period > 0) {
            val now = Date().time
            if (isCleanupDue(now)) {
                Log.i(LOG_TAG, "Performing data retention maintenance...")
                deleteSince(getThreshold(now))
                updateLastCleanup(now)
            }
        }
    }

    private fun getLastCleanup(fallback: Long): Long {
        if (lastCleanup == 0L) {
            lastCleanup = prefs.getLong(KEY_LAST_CLEANUP, fallback)
        }
        return lastCleanup
    }

    private fun updateLastCleanup(time: Long) {
        lastCleanup = time
        prefs.edit().putLong(KEY_LAST_CLEANUP, time).apply()
    }

    private fun deleteSince(threshold: Long) {
        val rows = context.contentResolver.delete(ChuckContentProvider.TRANSACTION_URI,
                "requestDate <= ?", arrayOf(threshold.toString()))
        Log.i(LOG_TAG, "$rows transactions deleted")
    }

    private fun isCleanupDue(now: Long): Boolean {
        return now - getLastCleanup(now) > cleanupFrequency
    }

    private fun getThreshold(now: Long): Long {
        return if (period == 0L) now else now - period
    }

    private fun toMillis(period: ChuckInterceptor.Period): Long {
        return when (period) {
            ChuckInterceptor.Period.ONE_HOUR -> TimeUnit.HOURS.toMillis(1)
            ChuckInterceptor.Period.ONE_DAY -> TimeUnit.DAYS.toMillis(1)
            ChuckInterceptor.Period.ONE_WEEK -> TimeUnit.DAYS.toMillis(7)
            else -> 0
        }
    }

    companion object {

        private const val LOG_TAG = "Chuck"
        private const val PREFS_NAME = "chuck_preferences"
        private const val KEY_LAST_CLEANUP = "last_cleanup"

        private var lastCleanup: Long = 0
    }
}
