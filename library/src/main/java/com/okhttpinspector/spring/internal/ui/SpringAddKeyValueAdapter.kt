package com.okhttpinspector.spring.internal.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.okhttpinspector.spring.R
import kotlinx.android.synthetic.main.spring_list_item_add.view.*
import kotlinx.android.synthetic.main.spring_list_item_key_value.view.*

internal class SpringAddKeyValueAdapter(private val addKeyValuePair: ArrayList<Pair<String, String>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val edittext = SpringKeyValueVH(LayoutInflater.from(parent.context).inflate(R.layout.spring_list_item_key_value, parent, false))
        val addedittext = SpringAddKeyValueVH(LayoutInflater.from(parent.context).inflate(R.layout.spring_list_item_add, parent, false))
        return if (viewType == 1) addedittext else edittext
    }

    override fun getItemViewType(position: Int): Int {
        return if (itemCount - 1 == position) 1
        else 0
    }

    override fun getItemCount(): Int {
        return addKeyValuePair.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SpringKeyValueVH) {
            holder.onHeaderChanged = {
                val pos = holder.adapterPosition
                val pair = addKeyValuePair.removeAt(pos)
                addKeyValuePair.add(pos, Pair(it.toString(), pair.second))
            }
            holder.onValueChanged = {
                val pos = holder.adapterPosition
                val pair = addKeyValuePair.removeAt(pos)
                addKeyValuePair.add(pos, Pair(pair.first, it.toString()))
            }
            holder.itemView.spring_remove_btn.setOnClickListener {
                val pos = holder.adapterPosition
                addKeyValuePair.removeAt(pos)
                notifyItemRemoved(pos)
            }
        } else if (holder is SpringAddKeyValueVH) {
            holder.itemView.spring_add_more_btn.setOnClickListener {
                val insertPos = addKeyValuePair.size
                addKeyValuePair.add(Pair("", ""))
                notifyItemInserted(insertPos)
            }
        }
    }

    internal inner class SpringKeyValueVH(view: View) : RecyclerView.ViewHolder(view) {
        var onHeaderChanged: ((Editable?) -> Unit)? = null
        var onValueChanged: ((Editable?) -> Unit)? = null

        init {
            itemView.spring_header.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    onHeaderChanged?.invoke(s)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

            })
            itemView.spring_value.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    onValueChanged?.invoke(s)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

            })
        }
    }

    internal inner class SpringAddKeyValueVH(view: View) : RecyclerView.ViewHolder(view)
}