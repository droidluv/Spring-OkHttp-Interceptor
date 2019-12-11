package com.okhttpinspector.spring.internal.ui

import android.content.Intent
import android.os.Bundle
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.Spring
import com.okhttpinspector.spring.internal.support.getColorResource
import com.okhttpinspector.spring.internal.support.parseColor
import kotlinx.android.synthetic.main.spring_activity_override.*

class SpringOverrideListActivity : SpringBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spring_activity_override)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setBackgroundColor(Spring.actionBarColorHex.parseColor(this getColorResource R.color.spring_colorPrimary))

        refresh_comments_fab.setOnClickListener {
            startActivity(Intent(this@SpringOverrideListActivity, SpringOverrideRuleCreatorActivity::class.java))
        }

    }

}