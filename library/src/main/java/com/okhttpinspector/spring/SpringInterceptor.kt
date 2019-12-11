/*
 * Copyright (C) 2015 Square, Inc, 2019 Sebi Sheldin Sebastian.
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
package com.okhttpinspector.spring

import android.content.Context
import android.net.Uri
import android.util.Log
import com.okhttpinspector.spring.internal.data.HttpTransaction
import com.okhttpinspector.spring.internal.data.LocalCupboard
import com.okhttpinspector.spring.internal.data.SpringContentProvider
import com.okhttpinspector.spring.internal.support.NotificationHelper
import com.okhttpinspector.spring.internal.support.RetentionManager
import okhttp3.*
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.BufferedSource
import okio.GzipSource
import okio.buffer
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * An OkHttp Interceptor which persists and displays HTTP activity in your application for later inspection.
 *
 * @param context The current Context.
 */
class SpringInterceptor(context: Context) : Interceptor {

    private val context: Context = context.applicationContext
    private val notificationHelper: NotificationHelper
    private var retentionManager: RetentionManager? = null
    private var showNotification: Boolean = false
    private var maxContentLength = 250000L

    enum class Period {
        /**
         * Retain data for the last hour.
         */
        ONE_HOUR,
        /**
         * Retain data for the last day.
         */
        ONE_DAY,
        /**
         * Retain data for the last week.
         */
        ONE_WEEK,
        /**
         * Retain data forever.
         */
        FOREVER
    }

    init {
        notificationHelper = NotificationHelper(this.context)
        showNotification = true
        retentionManager = RetentionManager(this.context, DEFAULT_RETENTION)
    }

    /**
     * Control whether a notification is shown while HTTP activity is recorded.
     *
     * @param show true to show a notification, false to suppress it.
     * @return The [SpringInterceptor] instance.
     */
    fun showNotification(show: Boolean): SpringInterceptor {
        showNotification = show
        return this
    }

    /**
     * Set the maximum length for request and response content before it is truncated.
     * Warning: setting this value too high may cause unexpected results.
     *
     * @param max the maximum length (in bytes) for request/response content.
     * @return The [SpringInterceptor] instance.
     */
    fun maxContentLength(max: Long): SpringInterceptor {
        this.maxContentLength = max
        return this
    }

    /**
     * Set the retention period for HTTP transaction data captured by this interceptor.
     * The default is one week.
     *
     * @param period the peroid for which to retain HTTP transaction data.
     * @return The [SpringInterceptor] instance.
     */
    fun retainDataFor(period: Period): SpringInterceptor {
        retentionManager = RetentionManager(context, period)
        return this
    }

    private fun updateRequestIfNeeded(request: Request): Request {
        val updatedRequest = request.newBuilder()

        val originalUrl = request.url.toString()

        updatedRequest.method(request.method, request.body)
        return updatedRequest.build()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = updateRequestIfNeeded(chain.request())

        val requestBody: RequestBody? = request.body
        val hasRequestBody = requestBody != null

        val transaction = HttpTransaction()
        transaction.requestDate = Date()

        transaction.method = request.method
        transaction.url = request.url.toString()

        transaction.setRequestHeaders(request.headers)
        if (hasRequestBody) {
            if (requestBody?.contentType() != null) {
                transaction.requestContentType = requestBody.contentType().toString()
            }
            if (requestBody?.contentLength() != -1L) {
                transaction.requestContentLength = requestBody?.contentLength()
            }
        }

        transaction.setRequestBodyIsPlainText(!bodyHasUnsupportedEncoding(request.headers))
        if (hasRequestBody && transaction.requestBodyIsPlainText()) {
            val source = getNativeSource(Buffer(), bodyGzipped(request.headers))
            val buffer = source.buffer
            requestBody?.writeTo(buffer)
            var charset = UTF8
            val contentType = requestBody?.contentType()
            if (contentType != null) {
                charset = contentType.charset(UTF8)
            }
            if (isPlaintext(buffer)) {
                transaction.requestBody = readFromBuffer(buffer, charset)
            } else {
                transaction.setResponseBodyIsPlainText(false)
            }
        }

        val transactionUri = create(transaction)

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            transaction.error = e.toString()
            update(transaction, transactionUri)
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody: ResponseBody? = response.body

        transaction.setRequestHeaders(response.request.headers) // includes headers added later in the chain
        transaction.responseDate = Date()
        transaction.tookMs = tookMs
        transaction.protocol = response.protocol.toString()
        transaction.responseCode = response.code
        transaction.responseMessage = response.message

        transaction.responseContentLength = responseBody?.contentLength()
        if (responseBody?.contentType() != null) {
            transaction.responseContentType = responseBody.contentType().toString()
        }
        transaction.setResponseHeaders(response.headers)

        transaction.setResponseBodyIsPlainText(!bodyHasUnsupportedEncoding(response.headers))
        if (response.promisesBody() && transaction.responseBodyIsPlainText()) {
            val source = getNativeSource(response)
            source?.request(java.lang.Long.MAX_VALUE)
            val buffer = source?.buffer
            var charset = UTF8
            val contentType = responseBody?.contentType()
            if (contentType != null) try {
                charset = contentType.charset(UTF8)
            } catch (e: UnsupportedCharsetException) {
                update(transaction, transactionUri)
                return response
            }
            if (buffer != null && isPlaintext(buffer)) {
                transaction.responseBody = readFromBuffer(buffer.clone(), charset)
            } else {
                transaction.setResponseBodyIsPlainText(false)
            }
            transaction.responseContentLength = buffer?.size
        }

        update(transaction, transactionUri)

        return response
    }

    private fun create(transaction: HttpTransaction): Uri {
        val values = LocalCupboard.instance.withEntity(HttpTransaction::class.java).toContentValues(transaction)
        val uri = context.contentResolver.insert(SpringContentProvider.TRANSACTION_URI, values)
        transaction.id = java.lang.Long.valueOf(uri?.lastPathSegment?:"0")
        if (showNotification) {
            notificationHelper.show(transaction)
        }
        retentionManager?.doMaintenance()
        return uri ?: Uri.EMPTY
    }

    private fun update(transaction: HttpTransaction, uri: Uri): Int {
        val values = LocalCupboard.instance.withEntity(HttpTransaction::class.java).toContentValues(transaction)
        val updated = context.contentResolver.update(uri, values, null, null)
        if (showNotification && updated > 0) {
            notificationHelper.show(transaction)
        }
        return updated
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (e: EOFException) {
            return false // Truncated UTF-8 sequence.
        }

    }

    private fun bodyHasUnsupportedEncoding(headers: Headers): Boolean {
        val contentEncoding: String? = headers.get("Content-Encoding")
        return contentEncoding != null &&
                !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun bodyGzipped(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"]
        return "gzip".equals(contentEncoding, ignoreCase = true)
    }

    private fun readFromBuffer(buffer: Buffer, charset: Charset): String {
        val bufferSize = buffer.size
        val maxBytes = Math.min(bufferSize, maxContentLength)
        var body = ""
        try {
            body = buffer.readString(maxBytes, charset)
        } catch (e: EOFException) {
            body += context.getString(R.string.spring_body_unexpected_eof)
        }

        if (bufferSize > maxContentLength) {
            body += context.getString(R.string.spring_body_content_truncated)
        }
        return body
    }

    private fun getNativeSource(input: BufferedSource, isGzipped: Boolean): BufferedSource {
        return if (isGzipped) {
            val source = GzipSource(input)
            source.buffer()
        } else input
    }

    @Throws(IOException::class)
    private fun getNativeSource(response: Response): BufferedSource? {
        if (bodyGzipped(response.headers)) {
            val source = response.peekBody(maxContentLength).source()
            if (source.buffer.size < maxContentLength) {
                return getNativeSource(source, true)
            } else {
                Log.w(LOG_TAG, "gzip encoded response was too long")
            }
        }
        return response.body?.source()
    }

    companion object {

        private const val LOG_TAG = "SpringInterceptor"
        private val DEFAULT_RETENTION = Period.ONE_WEEK
        private val UTF8 = Charset.forName("UTF-8")
    }
}
