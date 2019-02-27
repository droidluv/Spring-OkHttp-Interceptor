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
package com.readystatesoftware.chuck.internal.support

import android.content.Context
import android.text.TextUtils
import com.google.gson.JsonParser
import com.readystatesoftware.chuck.R
import com.readystatesoftware.chuck.internal.data.HttpHeader
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult

object FormatUtils {

    fun formatHeaders(httpHeaders: List<HttpHeader>?, withMarkup: Boolean): String {
        var out = ""
        if (httpHeaders != null) {
            for ((name, value) in httpHeaders) {
                out += (if (withMarkup) "<b>" else "") + name + ": " + (if (withMarkup) "</b>" else "") +
                        value + if (withMarkup) "<br />" else "\n"
            }
        }
        return out
    }

    fun formatByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun formatJson(json: String): String {
        return try {
            val jp = JsonParser()
            val je = jp.parse(json)
            JsonConverter.instance.toJson(je)
        } catch (e: Exception) {
            json
        }

    }

    fun formatXml(xml: String): String {
        return try {
            val serializer = SAXTransformerFactory.newInstance().newTransformer()
            serializer.setOutputProperty(OutputKeys.INDENT, "yes")
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val xmlSource = SAXSource(InputSource(ByteArrayInputStream(xml.toByteArray())))
            val res = StreamResult(ByteArrayOutputStream())
            serializer.transform(xmlSource, res)
            String((res.outputStream as ByteArrayOutputStream).toByteArray())
        } catch (e: Exception) {
            xml
        }

    }

    fun getShareText(context: Context, transaction: HttpTransaction): String {
        var text = ""
        text += context.getString(R.string.chuck_url) + ": " + v(transaction.url) + "\n"
        text += context.getString(R.string.chuck_method) + ": " + v(transaction.method) + "\n"
        text += context.getString(R.string.chuck_protocol) + ": " + v(transaction.protocol) + "\n"
        text += context.getString(R.string.chuck_status) + ": " + v(transaction.status.toString()) + "\n"
        text += context.getString(R.string.chuck_response) + ": " + v(transaction.responseSummaryText) + "\n"
        text += context.getString(R.string.chuck_ssl) + ": " + v(context.getString(if (transaction.isSsl) R.string.chuck_yes else R.string.chuck_no)) + "\n"
        text += "\n"
        text += context.getString(R.string.chuck_request_time) + ": " + v(transaction.requestDateString) + "\n"
        text += context.getString(R.string.chuck_response_time) + ": " + v(transaction.responseDateString) + "\n"
        text += context.getString(R.string.chuck_duration) + ": " + v(transaction.durationString) + "\n"
        text += "\n"
        text += context.getString(R.string.chuck_request_size) + ": " + v(transaction.requestSizeString) + "\n"
        text += context.getString(R.string.chuck_response_size) + ": " + v(transaction.responseSizeString) + "\n"
        text += context.getString(R.string.chuck_total_size) + ": " + v(transaction.totalSizeString) + "\n"
        text += "\n"
        text += "---------- " + context.getString(R.string.chuck_request) + " ----------\n\n"
        var headers = formatHeaders(transaction.getRequestHeaders(), false)
        if (!TextUtils.isEmpty(headers)) {
            text += headers + "\n"
        }
        text += if (transaction.requestBodyIsPlainText())
            v(transaction.formattedRequestBody)
        else
            context.getString(R.string.chuck_body_omitted)
        text += "\n\n"
        text += "---------- " + context.getString(R.string.chuck_response) + " ----------\n\n"
        headers = formatHeaders(transaction.getResponseHeaders(), false)
        if (!TextUtils.isEmpty(headers)) {
            text += headers + "\n"
        }
        text += if (transaction.responseBodyIsPlainText())
            v(transaction.formattedResponseBody)
        else
            context.getString(R.string.chuck_body_omitted)
        return text
    }

    fun getShareCurlCommand(transaction: HttpTransaction): String {
        var compressed = false
        var curlCmd = "curl"
        curlCmd += " -X " + transaction.method!!
        val headers = transaction.getRequestHeaders()
        var i = 0
        val count = headers!!.size
        while (i < count) {
            val name = headers[i].name
            val value = headers[i].value
            if ("Accept-Encoding".equals(name, ignoreCase = true) && "gzip".equals(value, ignoreCase = true)) {
                compressed = true
            }
            curlCmd += " -H \"$name: $value\""
            i++
        }
        val requestBody = transaction.requestBody
        if (requestBody != null && requestBody.length > 0) {
            // try to keep to a single line and use a subshell to preserve any line breaks
            curlCmd += " --data $'" + requestBody.replace("\n", "\\n") + "'"
        }
        curlCmd += (if (compressed) " --compressed " else " ") + transaction.url!!
        return curlCmd
    }

    private fun v(string: String?): String {
        return string ?: ""
    }
}