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

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.viewpager.widget.ViewPager
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring
import com.okhttpinspector.spring.internal.data.HttpTransaction
import com.okhttpinspector.spring.internal.data.LocalCupboard
import com.okhttpinspector.spring.internal.data.SpringContentProvider
import com.okhttpinspector.spring.internal.support.FormatUtils
import com.okhttpinspector.spring.internal.support.SimpleOnPageChangedListener
import com.okhttpinspector.spring.internal.support.getColorResource
import com.okhttpinspector.spring.internal.support.parseColor
import kotlinx.android.synthetic.main.spring_activity_transaction.*
import java.util.*

class SpringTransactionActivity : SpringBaseActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private var adapter: Adapter? = null
    private var transactionId: Long = 0
    private var transaction: HttpTransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spring_activity_transaction)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupViewPager(viewpager)
        tabs.setupWithViewPager(viewpager)

        tabs.setSelectedTabIndicatorColor(Spring.tabBarIndicatorColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))
        tabs.setBackgroundColor(Spring.tabBarBackgroundColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))
        toolbar.setBackgroundColor(Spring.actionBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))

        transactionId = intent.getLongExtra(ARG_TRANSACTION_ID, 0)
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.spring_transaction_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == R.id.share_text -> {
                share("Info of " + (transaction?.url
                        ?: "API Call"), FormatUtils.getShareText(this, transaction))
                true
            }
            item.itemId == R.id.share_curl -> {
                share("Curl for " + (transaction?.url
                        ?: "API Curl"), FormatUtils.getShareCurlCommand(transaction))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val loader = CursorLoader(this)
        loader.uri = ContentUris.withAppendedId(SpringContentProvider.TRANSACTION_URI, transactionId)
        return loader
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        transaction = LocalCupboard.instance.withCursor(data).get(HttpTransaction::class.java)
        populateUI()
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {}

    private fun populateUI() {
        val tempTransaction = transaction
        if (tempTransaction != null) {
            val textToSet = tempTransaction.method + " " + tempTransaction.path
            toolbar_title.text = textToSet
            for (fragment in (adapter?.fragmentSprings ?: emptyList<SpringTransactionFragment>())) {
                fragment.transactionUpdated(tempTransaction)
            }
        }
    }

    private fun setupViewPager(viewPager: ViewPager) {
        adapter = Adapter(supportFragmentManager)
        adapter?.addFragment(SpringTransactionOverviewFragment(), getString(R.string.spring_overview))
        adapter?.addFragment(SpringTransactionPayloadFragment.newInstance(SpringTransactionPayloadFragment.TYPE_REQUEST), getString(R.string.spring_request))
        adapter?.addFragment(SpringTransactionPayloadFragment.newInstance(SpringTransactionPayloadFragment.TYPE_RESPONSE), getString(R.string.spring_response))
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(object : SimpleOnPageChangedListener() {
            override fun onPageSelected(position: Int) {
                selectedTabPosition = position
            }
        })
        viewPager.currentItem = selectedTabPosition
    }

    private fun share(title: String, content: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        sendIntent.putExtra(Intent.EXTRA_TEXT, content)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, null))
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        val fragmentSprings: MutableList<SpringTransactionFragment> = ArrayList()
        private val fragmentTitles = ArrayList<String>()

        fun addFragment(fragmentSpring: SpringTransactionFragment, title: String) {
            fragmentSprings.add(fragmentSpring)
            fragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragmentSprings[position] as Fragment
        }

        override fun getCount(): Int {
            return fragmentSprings.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragmentTitles[position]
        }
    }

    companion object {

        private const val ARG_TRANSACTION_ID = "transaction_id"

        private var selectedTabPosition = 0

        fun start(context: Context, transactionId: Long) {
            val intent = Intent(context, SpringTransactionActivity::class.java)
            intent.putExtra(ARG_TRANSACTION_ID, transactionId)
            context.startActivity(intent)
        }
    }
}
