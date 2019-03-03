/*
 * Copyright (C) 2019 Sebi Sheldin Sebastian.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okhttpinspector.spring.internal.support

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.okhttpinspector.spring.SpringInterceptor
import com.okhttpinspector.spring.internal.data.SpringContentProvider
import java.util.*
import java.util.concurrent.TimeUnit

class RetentionManager(private val context: Context, retentionPeriod: SpringInterceptor.Period) {
    private val period: Long
    private val cleanupFrequency: Long
    private val prefs: SharedPreferences

    init {
        period = toMillis(retentionPeriod)
        prefs = context.getSharedPreferences(PREFS_NAME, 0)
        cleanupFrequency = if (retentionPeriod === SpringInterceptor.Period.ONE_HOUR)
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
        val rows = context.contentResolver.delete(SpringContentProvider.TRANSACTION_URI,
                "requestDate <= ?", arrayOf(threshold.toString()))
        Log.i(LOG_TAG, "$rows transactions deleted")
    }

    private fun isCleanupDue(now: Long): Boolean {
        return now - getLastCleanup(now) > cleanupFrequency
    }

    private fun getThreshold(now: Long): Long {
        return if (period == 0L) now else now - period
    }

    private fun toMillis(period: SpringInterceptor.Period): Long {
        return when (period) {
            SpringInterceptor.Period.ONE_HOUR -> TimeUnit.HOURS.toMillis(1)
            SpringInterceptor.Period.ONE_DAY -> TimeUnit.DAYS.toMillis(1)
            SpringInterceptor.Period.ONE_WEEK -> TimeUnit.DAYS.toMillis(7)
            else -> 0
        }
    }

    companion object {

        private const val LOG_TAG = "Spring"
        private const val PREFS_NAME = "spring_preferences"
        private const val KEY_LAST_CLEANUP = "last_cleanup"

        private var lastCleanup: Long = 0
    }
}
