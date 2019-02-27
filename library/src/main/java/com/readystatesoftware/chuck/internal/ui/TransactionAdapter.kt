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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.recyclerview.widget.RecyclerView
import com.readystatesoftware.chuck.R
import com.readystatesoftware.chuck.internal.data.HttpTransaction
import com.readystatesoftware.chuck.internal.data.LocalCupboard
import com.readystatesoftware.chuck.internal.ui.TransactionListFragment.OnListFragmentInteractionListener

internal class TransactionAdapter(private val context: Context, private val listener: OnListFragmentInteractionListener?) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
    private val cursorAdapter: CursorAdapter

    private val colorDefault: Int = ContextCompat.getColor(context, R.color.chuck_status_default)
    private val colorRequested: Int = ContextCompat.getColor(context, R.color.chuck_status_requested)
    private val colorError: Int = ContextCompat.getColor(context, R.color.chuck_status_error)
    private val color500: Int = ContextCompat.getColor(context, R.color.chuck_status_500)
    private val color400: Int = ContextCompat.getColor(context, R.color.chuck_status_400)
    private val color300: Int = ContextCompat.getColor(context, R.color.chuck_status_300)

    init {

        cursorAdapter = object : CursorAdapter(this@TransactionAdapter.context, null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.chuck_list_item_transaction, parent, false)
                val holder = ViewHolder(itemView)
                itemView.tag = holder
                return itemView
            }

            override fun bindView(view: View, context: Context, cursor: Cursor) {
                val transaction = LocalCupboard.instance.withCursor(cursor).get(HttpTransaction::class.java)
                val holder = view.tag as ViewHolder
                val pathTextToSet =  transaction.method + " " + transaction.path
                holder.path.text = pathTextToSet
                holder.host.text = transaction.host
                holder.start.text = transaction.requestStartTimeString
                holder.ssl.visibility = if (transaction.isSsl) View.VISIBLE else View.GONE
                if (transaction.status == HttpTransaction.Status.Complete) {
                    holder.code.text = transaction.responseCode.toString()
                    holder.duration.text = transaction.durationString
                    holder.size.text = transaction.totalSizeString
                } else {
                    holder.code.text = null
                    holder.duration.text = null
                    holder.size.text = null
                }
                if (transaction.status == HttpTransaction.Status.Failed) {
                    holder.code.text = "!!!"
                }
                setStatusColor(holder, transaction)
                holder.transaction = transaction
                holder.view.setOnClickListener {
                    if (null != this@TransactionAdapter.listener) {
                        this@TransactionAdapter.listener.onListFragmentInteraction(holder.transaction!!)
                    }
                }
            }

            private fun setStatusColor(holder: ViewHolder, transaction: HttpTransaction) {
                val color: Int = when {
                    transaction.status == HttpTransaction.Status.Failed -> colorError
                    transaction.status == HttpTransaction.Status.Requested -> colorRequested
                    (transaction.responseCode?:0) >= 500 -> color500
                    (transaction.responseCode?:0) >= 400 -> color400
                    (transaction.responseCode?:0) >= 300 -> color300
                    else -> colorDefault
                }
                holder.code.setTextColor(color)
                holder.path.setTextColor(color)
            }
        }
    }

    override fun getItemCount(): Int {
        return cursorAdapter.count
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        cursorAdapter.cursor.moveToPosition(position)
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.cursor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = cursorAdapter.newView(context, cursorAdapter.cursor, parent)
        return ViewHolder(v)
    }

    fun swapCursor(newCursor: Cursor?) {
        cursorAdapter.swapCursor(newCursor)
        notifyDataSetChanged()
    }

    internal inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val code: TextView = view.findViewById(R.id.code)
        val path: TextView = view.findViewById(R.id.path)
        val host: TextView = view.findViewById(R.id.host)
        val start: TextView = view.findViewById(R.id.start)
        val duration: TextView = view.findViewById(R.id.duration)
        val size: TextView = view.findViewById(R.id.size)
        val ssl: ImageView = view.findViewById(R.id.ssl)
        var transaction: HttpTransaction? = null

    }
}
