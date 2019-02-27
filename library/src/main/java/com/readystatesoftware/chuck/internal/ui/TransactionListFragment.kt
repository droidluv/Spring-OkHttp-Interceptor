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

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.readystatesoftware.chuck.R
import com.readystatesoftware.chuck.internal.data.ChuckContentProvider
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import com.readystatesoftware.chuck.internal.support.NotificationHelper
import com.readystatesoftware.chuck.internal.support.SQLiteUtils

class TransactionListFragment : Fragment(), SearchView.OnQueryTextListener {

    private var currentFilter: String? = null
    private var listener: OnListFragmentInteractionListener? = null
    private var adapter: TransactionAdapter? = null
    private lateinit var loaderListener: LoaderManager.LoaderCallbacks<Cursor>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.chuck_fragment_transaction_list, container, false)
        if (view is RecyclerView) {
            val context = view.context
            view.layoutManager = LinearLayoutManager(context)
            view.addItemDecoration(DividerItemDecoration(getContext()!!,
                    DividerItemDecoration.VERTICAL))
            adapter = TransactionAdapter(context, listener)
            view.adapter = adapter
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let {
            loaderListener = object: LoaderManager.LoaderCallbacks<Cursor>{
                override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
                    val loader = CursorLoader(it)
                    loader.uri = ChuckContentProvider.TRANSACTION_URI
                    if (!TextUtils.isEmpty(currentFilter)) {
                        if (TextUtils.isDigitsOnly(currentFilter)) {
                            loader.selection = "responseCode LIKE ?"
                            loader.selectionArgs = arrayOf(currentFilter!! + "%")
                        } else {
                            loader.selection = "path LIKE ?"
                            loader.selectionArgs = arrayOf("%$currentFilter%")
                        }
                    }
                    loader.projection = HttpTransaction.PARTIAL_PROJECTION
                    loader.sortOrder = "requestDate DESC"
                    return loader
                }

                override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
                    adapter?.swapCursor(data)
                }

                override fun onLoaderReset(loader: Loader<Cursor>) {
                    adapter?.swapCursor(null)
                }

            }
            LoaderManager.getInstance(this).initLoader(0, null, loaderListener)
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chuck_main, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setIconifiedByDefault(true)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == R.id.clear -> {
                context?.contentResolver?.delete(ChuckContentProvider.TRANSACTION_URI, null, null)
                NotificationHelper.clearBuffer()
                true
            }
            item.itemId == R.id.browse_sql -> {
                context?.let {
                    SQLiteUtils.browseDatabase(it)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        currentFilter = newText
        if(::loaderListener.isInitialized)
            LoaderManager.getInstance(this).restartLoader(0, null, loaderListener)
        return true
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: HttpTransaction)
    }

    companion object {

        internal fun newInstance(): TransactionListFragment {
            return TransactionListFragment()
        }
    }
}
