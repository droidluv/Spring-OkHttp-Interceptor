/*
 * Copyright (C) 2017 Jeff Gilfelt.
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
package com.readystatesoftware.chuck.internal.data

import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri

class ChuckContentProvider : ContentProvider() {

    private var databaseHelper: ChuckDbOpenHelper? = null

    override fun attachInfo(context: Context, info: ProviderInfo) {
        super.attachInfo(context, info)
        TRANSACTION_URI = Uri.parse("content://" + info.authority + "/transaction")
        matcher.addURI(info.authority, "transaction/#", TRANSACTION)
        matcher.addURI(info.authority, "transaction", TRANSACTIONS)
    }

    override fun onCreate(): Boolean {
        context?.apply { databaseHelper = ChuckDbOpenHelper(this) }
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?,
                       selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        val db = databaseHelper?.writableDatabase
        val projectionTemp = projection ?: emptyArray()
        val selectionArgsTemp = selectionArgs ?: emptyArray()
        var cursor: Cursor? = null
        when (matcher.match(uri)) {
            TRANSACTIONS -> cursor = LocalCupboard.instance.withDatabase(db)?.query(HttpTransaction::class.java)?.withProjection(*projectionTemp)?.withSelection(selection, *selectionArgsTemp)?.orderBy(sortOrder)?.cursor
            TRANSACTION -> cursor = LocalCupboard.instance.withDatabase(db)?.query(HttpTransaction::class.java)?.byId(ContentUris.parseId(uri))?.cursor
        }
        cursor?.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val db = databaseHelper!!.writableDatabase
        when (matcher.match(uri)) {
            TRANSACTIONS -> {
                val id = db.insert(LocalCupboard.instance.getTable(HttpTransaction::class.java), null, contentValues)
                if (id > 0) {
                    context?.contentResolver?.notifyChange(uri, null)
                    return ContentUris.withAppendedId(TRANSACTION_URI, id)
                }
            }
        }
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = databaseHelper!!.writableDatabase
        var result = 0
        when (matcher.match(uri)) {
            TRANSACTIONS -> result = db.delete(LocalCupboard.instance.getTable(HttpTransaction::class.java), selection, selectionArgs)
            TRANSACTION -> result = db.delete(LocalCupboard.instance.getTable(HttpTransaction::class.java),
                    "_id = ?", arrayOf(uri.pathSegments[1]))
        }
        if (result > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        return result
    }

    override fun update(uri: Uri, contentValues: ContentValues?,
                        selection: String?, selectionArgs: Array<String>?): Int {
        val db = databaseHelper!!.writableDatabase
        var result = 0
        when (matcher.match(uri)) {
            TRANSACTIONS -> result = db.update(LocalCupboard.instance.getTable(HttpTransaction::class.java), contentValues, selection, selectionArgs)
            TRANSACTION -> result = db.update(LocalCupboard.instance.getTable(HttpTransaction::class.java), contentValues,
                    "_id = ?", arrayOf(uri.pathSegments[1]))
        }
        if (result > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        }
        return result
    }

    companion object {

        var TRANSACTION_URI: Uri = Uri.EMPTY

        private const val TRANSACTION = 0
        private const val TRANSACTIONS = 1
        private val matcher = UriMatcher(UriMatcher.NO_MATCH)
    }
}
