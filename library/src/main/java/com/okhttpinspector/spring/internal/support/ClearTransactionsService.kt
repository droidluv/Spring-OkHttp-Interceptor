package com.okhttpinspector.spring.internal.support

import android.app.IntentService
import android.content.Intent
import com.okhttpinspector.spring.internal.data.SpringContentProvider

class ClearTransactionsService : IntentService("Spring-ClearTransactionsService") {

    override fun onHandleIntent(intent: Intent?) {
        contentResolver.delete(SpringContentProvider.TRANSACTION_URI, null, null)
        NotificationHelper.clearBuffer()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.dismiss()
    }
}