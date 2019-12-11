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
package com.okhttpinspector.spring.internal.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring

import com.okhttpinspector.spring.internal.support.NotificationHelper
import com.okhttpinspector.spring.internal.support.changeStatusBarColor
import com.okhttpinspector.spring.internal.support.getColorResource
import com.okhttpinspector.spring.internal.support.parseColor

abstract class SpringBaseActivity : AppCompatActivity() {

    private var notificationHelper: NotificationHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        changeStatusBarColor(Spring.statusBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimaryDark), false)
        requestedOrientation = if (Spring.allowOrientationChange) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        notificationHelper = NotificationHelper(this)
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
        notificationHelper!!.dismiss()
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    companion object {
        var isInForeground: Boolean = false
    }

}
