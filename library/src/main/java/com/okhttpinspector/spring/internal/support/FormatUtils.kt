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
import android.text.TextUtils
import com.google.gson.JsonParser.parseString
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.internal.data.HttpHeader
import com.okhttpinspector.spring.internal.data.HttpTransaction
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.sax.SAXSource
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.stream.StreamResult
import kotlin.math.ln
import kotlin.math.pow

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
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format(Locale.US, "%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    fun formatJson(json: String?): String? {
        return try {
            JsonConverter.instance.toJson(parseString(json))
        } catch (e: Exception) {
            json
        }

    }

    fun formatXml(xml: String?): String? {
        return try {
            val serializer = SAXTransformerFactory.newInstance().newTransformer()
            serializer.setOutputProperty(OutputKeys.INDENT, "yes")
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val xmlSource = SAXSource(InputSource(ByteArrayInputStream(xml?.toByteArray())))
            val res = StreamResult(ByteArrayOutputStream())
            serializer.transform(xmlSource, res)
            String((res.outputStream as ByteArrayOutputStream).toByteArray())
        } catch (e: Exception) {
            xml
        }

    }

    fun getShareText(context: Context, transaction: HttpTransaction?): String {
        var text = ""
        text += context.getString(R.string.spring_url) + ": " + v(transaction?.url) + "\n"
        text += context.getString(R.string.spring_method) + ": " + v(transaction?.method) + "\n"
        text += context.getString(R.string.spring_protocol) + ": " + v(transaction?.protocol) + "\n"
        text += context.getString(R.string.spring_status) + ": " + v(transaction?.status.toString()) + "\n"
        text += context.getString(R.string.spring_response) + ": " + v(transaction?.responseSummaryText) + "\n"
        text += context.getString(R.string.spring_ssl) + ": " + v(context.getString(if (transaction?.isSsl == true) R.string.spring_yes else R.string.spring_no)) + "\n"
        text += "\n"
        text += context.getString(R.string.spring_request_time) + ": " + v(transaction?.requestDateString) + "\n"
        text += context.getString(R.string.spring_response_time) + ": " + v(transaction?.responseDateString) + "\n"
        text += context.getString(R.string.spring_duration) + ": " + v(transaction?.durationString) + "\n"
        text += "\n"
        text += context.getString(R.string.spring_request_size) + ": " + v(transaction?.requestSizeString) + "\n"
        text += context.getString(R.string.spring_response_size) + ": " + v(transaction?.responseSizeString) + "\n"
        text += context.getString(R.string.spring_total_size) + ": " + v(transaction?.totalSizeString) + "\n"
        text += "\n"
        text += "---------- " + context.getString(R.string.spring_request) + " ----------\n\n"
        var headers = formatHeaders(transaction?.requestHeaders, false)
        if (!TextUtils.isEmpty(headers)) {
            text += headers + "\n"
        }
        text += if (transaction?.requestBodyIsPlainText() == true)
            v(transaction.formattedRequestBody)
        else
            context.getString(R.string.spring_body_omitted)
        text += "\n\n"
        text += "---------- " + context.getString(R.string.spring_response) + " ----------\n\n"
        headers = formatHeaders(transaction?.responseHeaders, false)
        if (!TextUtils.isEmpty(headers)) {
            text += headers + "\n"
        }
        text += if (transaction?.responseBodyIsPlainText() == true)
            v(transaction.formattedResponseBody)
        else
            context.getString(R.string.spring_body_omitted)
        return text
    }

    fun getShareCurlCommand(transaction: HttpTransaction?): String {
        var compressed = false
        var curlCmd = "curl"
        curlCmd += " -X " + v(transaction?.method)
        val headers = transaction?.requestHeaders ?: emptyList()
        var i = 0
        val count = headers.size
        while (i < count) {
            val name = headers[i].name
            val value = headers[i].value
            if ("Accept-Encoding".equals(name, ignoreCase = true) && "gzip".equals(value, ignoreCase = true)) {
                compressed = true
            }
            curlCmd += " -H \'$name: $value\'"
            i++
        }
        val requestBody = v(transaction?.requestBody)
        if (requestBody.isNotEmpty()) {
            // try to keep to a single line and use a subshell to preserve any line breaks
            curlCmd += " -d '" + requestBody.replace("\n", "\\n") + "'"
        }
        curlCmd += (if (compressed) " --compressed " else " ") + "\'${v(transaction?.url)}\'"
        return curlCmd
    }

    private fun v(string: String?): String {
        return string ?: ""
    }
}