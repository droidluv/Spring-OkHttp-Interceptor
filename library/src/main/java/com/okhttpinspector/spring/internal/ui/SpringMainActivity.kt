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
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring
import com.okhttpinspector.spring.internal.data.HttpTransaction
import com.okhttpinspector.spring.internal.support.changeStatusBarColor
import com.okhttpinspector.spring.internal.support.getColorResource
import com.okhttpinspector.spring.internal.support.parseColor
import kotlinx.android.synthetic.main.spring_activity_main.*

class SpringMainActivity : SpringBaseActivity(), SpringTransactionListFragment.OnListFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spring_activity_main)

        requestedOrientation = if(Spring.allowOrientationChange) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setSupportActionBar(toolbar)
        changeStatusBarColor(Spring.statusBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimaryDark ), false)

        toolbar.setBackgroundColor(Spring.actionBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))
        if (Spring.subtitle != null) toolbar.subtitle = Spring.subtitle
        supportActionBar?.title = Spring.title ?: getString(R.string.spring_name)
        this.title = Spring.title ?: getString(R.string.spring_name)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, SpringTransactionListFragment.newInstance())
                    .commit()
        }
    }

    override fun onListFragmentInteraction(item: HttpTransaction) {
        SpringTransactionActivity.start(this, item.id)
    }
}
