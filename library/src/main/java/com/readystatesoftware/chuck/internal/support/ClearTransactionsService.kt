package com.readystatesoftware.chuck.internal.support

import android.app.IntentService
import android.content.Intent
import com.readystatesoftware.chuck.internal.data.ChuckContentProvider

class ClearTransactionsService : IntentService("Chuck-ClearTransactionsService") {

    override fun onHandleIntent(intent: Intent?) {
        contentResolver.delete(ChuckContentProvider.TRANSACTION_URI, null, null)
        NotificationHelper.clearBuffer()
        val notificationHelper = NotificationHelper(this)
        notificationHelper.dismiss()
    }
}