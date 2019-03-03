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

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.okhttpinspector.spring.R
import com.okhttpinspector.spring.internal.data.HttpTransaction
import kotlinx.android.synthetic.main.spring_fragment_transaction_payload.*

class SpringTransactionPayloadFragment : Fragment(), SpringTransactionFragment {

    private var type: Int = 0
    private var transaction: HttpTransaction? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getInt(ARG_TYPE) ?: 0
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.spring_fragment_transaction_payload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateUI()
    }

    override fun transactionUpdated(transaction: HttpTransaction) {
        this.transaction = transaction
        populateUI()
    }

    private fun populateUI() {
        val tempTransaction = transaction
        if (isAdded && tempTransaction != null) {
            when (type) {
                TYPE_REQUEST -> setText(tempTransaction.getRequestHeadersString(true),
                        tempTransaction.formattedRequestBody, tempTransaction.requestBodyIsPlainText())
                TYPE_RESPONSE -> setText(tempTransaction.getResponseHeadersString(true),
                        tempTransaction.formattedResponseBody, tempTransaction.responseBodyIsPlainText())
            }
        }
    }

    private fun setText(headersString: String, bodyString: String?, isPlainText: Boolean) {
        headers?.visibility = if (TextUtils.isEmpty(headersString)) View.GONE else View.VISIBLE
        headers?.text = Html.fromHtml(headersString)
        if (!isPlainText) {
            body?.text = getString(R.string.spring_body_omitted)
        } else {
            body?.text = bodyString
        }
    }

    companion object {

        internal const val TYPE_REQUEST = 0
        internal const val TYPE_RESPONSE = 1

        private const val ARG_TYPE = "type"

        internal fun newInstance(type: Int): SpringTransactionPayloadFragment {
            val fragment = SpringTransactionPayloadFragment()
            val b = Bundle()
            b.putInt(ARG_TYPE, type)
            fragment.arguments = b
            return fragment
        }
    }
}