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
package com.readystatesoftware.chuck

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * No-op implementation.
 */
class ChuckInterceptor(context: Context) : Interceptor {

    enum class Period {
        ONE_HOUR,
        ONE_DAY,
        ONE_WEEK,
        FOREVER
    }

    fun showNotification(show: Boolean): ChuckInterceptor {
        return this
    }

    fun maxContentLength(max: Long): ChuckInterceptor {
        return this
    }

    fun retainDataFor(period: Period): ChuckInterceptor {
        return this
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}
