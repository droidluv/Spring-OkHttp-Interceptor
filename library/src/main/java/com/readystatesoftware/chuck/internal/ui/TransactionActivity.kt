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
package com.readystatesoftware.chuck.internal.ui

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
import com.readystatesoftware.chuck.R
import com.readystatesoftware.chuck.internal.data.ChuckContentProvider
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import com.readystatesoftware.chuck.internal.data.LocalCupboard
import com.readystatesoftware.chuck.internal.support.FormatUtils
import com.readystatesoftware.chuck.internal.support.SimpleOnPageChangedListener
import kotlinx.android.synthetic.main.chuck_activity_transaction.*
import java.util.*

class TransactionActivity : BaseChuckActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private var adapter: Adapter? = null
    private var transactionId: Long = 0
    private var transaction: HttpTransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chuck_activity_transaction)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupViewPager(viewpager)
        tabs.setupWithViewPager(viewpager)

        transactionId = intent.getLongExtra(ARG_TRANSACTION_ID, 0)
        LoaderManager.getInstance(this).initLoader(0, null, this)
    }

    override fun onResume() {
        super.onResume()
        LoaderManager.getInstance(this).restartLoader(0, null, this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.chuck_transaction, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == R.id.share_text -> {
                share(FormatUtils.getShareText(this, transaction!!))
                true
            }
            item.itemId == R.id.share_curl -> {
                share(FormatUtils.getShareCurlCommand(transaction!!))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val loader = CursorLoader(this)
        loader.uri = ContentUris.withAppendedId(ChuckContentProvider.TRANSACTION_URI, transactionId)
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
            for (fragment in (adapter?.fragments ?: emptyList<TransactionFragment>())) {
                fragment.transactionUpdated(tempTransaction)
            }
        }
    }

    private fun setupViewPager(viewPager: ViewPager) {
        adapter = Adapter(supportFragmentManager)
        adapter?.addFragment(TransactionOverviewFragment(), getString(R.string.chuck_overview))
        adapter?.addFragment(TransactionPayloadFragment.newInstance(TransactionPayloadFragment.TYPE_REQUEST), getString(R.string.chuck_request))
        adapter?.addFragment(TransactionPayloadFragment.newInstance(TransactionPayloadFragment.TYPE_RESPONSE), getString(R.string.chuck_response))
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(object : SimpleOnPageChangedListener() {
            override fun onPageSelected(position: Int) {
                selectedTabPosition = position
            }
        })
        viewPager.currentItem = selectedTabPosition
    }

    private fun share(content: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, content)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, null))
    }

    internal class Adapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        val fragments: MutableList<TransactionFragment> = ArrayList()
        private val fragmentTitles = ArrayList<String>()

        fun addFragment(fragment: TransactionFragment, title: String) {
            fragments.add(fragment)
            fragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position] as Fragment
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return fragmentTitles[position]
        }
    }

    companion object {

        private const val ARG_TRANSACTION_ID = "transaction_id"

        private var selectedTabPosition = 0

        fun start(context: Context, transactionId: Long) {
            val intent = Intent(context, TransactionActivity::class.java)
            intent.putExtra(ARG_TRANSACTION_ID, transactionId)
            context.startActivity(intent)
        }
    }
}
