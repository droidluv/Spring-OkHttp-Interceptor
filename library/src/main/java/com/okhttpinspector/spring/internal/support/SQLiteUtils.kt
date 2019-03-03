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
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object SQLiteUtils {

    fun browseDatabase(context: Context) {
        if (isIntentResolvable(context, getSQLiteDebuggerAppIntent("/"))) {
            val path = extractDatabase(context)
            if (path != null) {
                val intent = getSQLiteDebuggerAppIntent(path)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Unable to extract database", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Unable to resolve a SQLite Intent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractDatabase(context: Context): String? {
        try {
            val external = context.getExternalFilesDir(null)
            val data = Environment.getDataDirectory()
            if (external != null && external.canWrite()) {
                val dataDBPath = "data/" + context.packageName + "/databases/spring.db"
                val extractDBPath = "springdb.temp"
                val dataDB = File(data, dataDBPath)
                val extractDB = File(external, extractDBPath)
                if (dataDB.exists()) {
                    val `in` = FileInputStream(dataDB).channel
                    val out = FileOutputStream(extractDB).channel
                    out.transferFrom(`in`, 0, `in`.size())
                    `in`.close()
                    out.close()
                    return extractDB.absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun getSQLiteDebuggerAppIntent(path: String): Intent {
        val intent = Intent(Intent.ACTION_EDIT)
        intent.data = Uri.parse("sqlite:$path")
        return intent
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return context.packageManager.resolveActivity(intent, 0) != null
    }
}
