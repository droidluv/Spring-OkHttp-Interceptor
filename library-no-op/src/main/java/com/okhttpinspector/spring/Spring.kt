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
package com.okhttpinspector.spring

import android.content.Context
import android.content.Intent

/**
 * No-op implementation.
 */
object Spring {

    /**
     *   No Op
     */
    var subtitle: String? = null

    /**
     *
     */
    var title: String? = null

    /**
     *  No Op
     */
    var statusBarColorHex: String = "#0524ad"

    /**
     *  No Op
     */
    var actionBarColorHex: String = "#1234d0"

    /**
     *  No Op
     */
    var tabBarBackgroundColorHex: String = "#1234d0"
    /**
     *  No Op
     */
    var tabBarIndicatorColorHex: String = "#2ce049"

    /**
     * No Op
     */
    var allowOrientationChange: Boolean = false

    /**
     * No Op
     */
    fun getLaunchIntent(context: Context): Intent {
        return Intent()
    }
}
