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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.LongSparseArray
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring
import com.okhttpinspector.spring.internal.data.HttpTransaction
import com.okhttpinspector.spring.internal.ui.SpringBaseActivity
import java.lang.reflect.Method

class NotificationHelper(private val context: Context) {
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val clearAction: NotificationCompat.Action
        get() {
            val clearTitle = context.getString(R.string.spring_clear)
            val deleteIntent = Intent(context, ClearTransactionsService::class.java)
            val intent = PendingIntent.getService(context, 11, deleteIntent, PendingIntent.FLAG_ONE_SHOT)
            return NotificationCompat.Action(R.drawable.spring_vector_delete,
                    clearTitle, intent)
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID,
                            context.getString(R.string.spring_notification_category), NotificationManager.IMPORTANCE_LOW))
        }
    }

    @Synchronized
    fun show(transaction: HttpTransaction) {
        addToBuffer(transaction)
        if (!SpringBaseActivity.isInForeground) {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentIntent(PendingIntent.getActivity(context, 0, Spring.getLaunchIntent(context), 0))
                    .setLocalOnly(true)
                    .setSmallIcon(R.drawable.spring_vector_upload_download)
                    .setColor(ContextCompat.getColor(context, R.color.spring_colorPrimary))
                    .setContentTitle(context.getString(R.string.spring_notification_title))
            val inboxStyle = NotificationCompat.InboxStyle()
            for ((count, i) in (transactionBuffer.size() - 1 downTo 0).withIndex()) {
                if (count < BUFFER_SIZE) {
                    if (count == 0) {
                        builder.setContentText(transactionBuffer.valueAt(i).notificationText)
                    }
                    inboxStyle.addLine(transactionBuffer.valueAt(i).notificationText)
                }
            }
            builder.setAutoCancel(true)
            builder.setStyle(inboxStyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setSubText(transactionCount.toString())
            } else {
                builder.setNumber(transactionCount)
            }
            builder.addAction(clearAction)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {

        private const val CHANNEL_ID = "spring"
        private const val NOTIFICATION_ID = 1230
        private const val BUFFER_SIZE = 10

        private val transactionBuffer = LongSparseArray<HttpTransaction>()
        private var transactionCount: Int = 0

        @Synchronized
        fun clearBuffer() {
            transactionBuffer.clear()
            transactionCount = 0
        }

        @Synchronized
        private fun addToBuffer(transaction: HttpTransaction) {
            if (transaction.status === HttpTransaction.Status.Requested) {
                transactionCount++
            }
            transactionBuffer.put(transaction.id, transaction)
            if (transactionBuffer.size() > BUFFER_SIZE) {
                transactionBuffer.removeAt(0)
            }
        }
    }
}
