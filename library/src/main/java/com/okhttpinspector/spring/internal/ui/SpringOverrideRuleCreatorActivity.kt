package com.okhttpinspector.spring.internal.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring
import com.okhttpinspector.spring.internal.support.getColorResource
import com.okhttpinspector.spring.internal.support.parseColor
import kotlinx.android.synthetic.main.spring_activity_override_rules.*
import kotlinx.android.synthetic.main.spring_content_override_rules.*

class SpringOverrideRuleCreatorActivity : SpringBaseActivity() {

    private val listOfStrings = ArrayList<String>()
    private val listOfHeaders = ArrayList<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spring_activity_override_rules)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setBackgroundColor(Spring.actionBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))

        val stringsLM = LinearLayoutManager(this)
        extra_strings_rv.layoutManager = stringsLM
        extra_strings_rv.isNestedScrollingEnabled = false
        extra_strings_rv.isFocusableInTouchMode = false
        extra_strings_rv.setHasFixedSize(false)
        listOfStrings.add("")
        extra_strings_rv.adapter = SpringAddEditTextAdapter(listOfStrings)

        val replaceLM = LinearLayoutManager(this)
        replace_header_rv.layoutManager = replaceLM
        replace_header_rv.isNestedScrollingEnabled = false
        replace_header_rv.isFocusableInTouchMode = false
        replace_header_rv.setHasFixedSize(false)
        replace_header_rv.adapter = SpringAddKeyValueAdapter(listOfHeaders)


        override_rule_save_btn.setOnClickListener {
            Toast.makeText(this, listOfHeaders.toString(), Toast.LENGTH_SHORT).show()
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when {
            item.itemId == android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
