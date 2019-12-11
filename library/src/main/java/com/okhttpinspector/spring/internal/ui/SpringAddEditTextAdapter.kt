package com.okhttpinspector.spring.internal.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.okhttpinspector.spring.R
import kotlinx.android.synthetic.main.spring_list_item_add.view.*
import kotlinx.android.synthetic.main.spring_list_item_removable_edittext.view.*

internal class SpringAddEditTextAdapter(private val addItemsList: ArrayList<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val edittext = SpringEditTextVH(LayoutInflater.from(parent.context).inflate(R.layout.spring_list_item_removable_edittext, parent, false))
        val addedittext = SpringAddEditTextVH(LayoutInflater.from(parent.context).inflate(R.layout.spring_list_item_add, parent, false))
        return if (viewType == 1) addedittext else edittext
    }

    override fun getItemViewType(position: Int): Int {
        return if (itemCount - 1 == position) 1
        else 0
    }

    override fun getItemCount(): Int {
        return addItemsList.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SpringEditTextVH) {
            holder.itemView.spring_edit_text.setText(addItemsList[position])
            holder.itemView.spring_remove_btn.setOnClickListener {
                val pos = holder.adapterPosition
                addItemsList.removeAt(pos)
                notifyItemRemoved(pos)
            }
            holder.onTextChanged = {
                val adapterPos = holder.adapterPosition
                addItemsList.removeAt(adapterPos)
                addItemsList.add(adapterPos, it.toString())
            }
            holder.itemView.spring_remove_btn.visibility = if (position == 0) View.GONE else View.VISIBLE
        } else if (holder is SpringAddEditTextVH) {
            holder.itemView.spring_add_more_btn.setOnClickListener {
                val insertPos = addItemsList.size
                addItemsList.add(insertPos, "")
                notifyItemInserted(insertPos)
            }
        }
    }

    internal inner class SpringEditTextVH(view: View) : RecyclerView.ViewHolder(view) {
        var onTextChanged: ((Editable?) -> Unit)? = null

        init {
            itemView.spring_edit_text.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    onTextChanged?.invoke(s)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

            })
        }
    }

    internal inner class SpringAddEditTextVH(view: View) : RecyclerView.ViewHolder(view)
}