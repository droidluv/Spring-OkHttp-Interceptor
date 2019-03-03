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
package com.okhttpinspector.spring

import android.content.Context
import android.content.Intent

import com.okhttpinspector.spring.internal.ui.SpringMainActivity

/**
 * Spring utilities.
 */
object Spring {

    /**
     * Get an Intent to launch the Spring UI directly.
     *
     * @param context A Context.
     * @return An Intent for the main Spring Activity that can be started with [Context.startActivity].
     */
    fun getLaunchIntent(context: Context): Intent {
        return Intent(context, SpringMainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}