/*
 * Copyright (C) 2018 The Android Open Source Project
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

@file:Suppress("UNCHECKED_CAST")

package com.braf.background

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.collections.set
import kotlin.math.round

private const val URL = "https://obmin.pro"

class ConvertViewModel(private val application: Application) : ViewModel() {

    //Test values
    private var testBuyListInMap = mutableMapOf("USD" to 28.80, "EUR" to 32.80, "RUB" to 0.365)

    private val currencies = mutableListOf("USD", "EUR", "RUB")

    val buyList = mutableMapOf<String, Double>()
    val sellList = mutableMapOf<String, Double>()

    fun calculate(currency1: String, currency2: String, input: String): String {
        return if (currency1 != "UAH" && currency2 != "UAH") {
            val currencyOfFrom = sellList[currency1]
            val currencyOfTo = sellList[currency2]
            val currencyOfToField = currencyOfFrom?.div(currencyOfTo!!)
            val inputOfFromField = input.toDouble()
            return inputOfFromField.times(currencyOfToField!!).round(2).toString()
        } else if (currency1 == "UAH") {
            val currencyOfToField = sellList[currency2]
            val inputOfFromField = input.toDouble()
            return inputOfFromField.div(currencyOfToField!!).round(2).toString()
        } else if (currency1 in currencies) {
            val currencyOfToField = sellList[currency1]
            val inputOfFromField = input.toDouble()
            inputOfFromField.times(currencyOfToField!!).round(2).toString()
        } else "00.00"
    }

    // PARSING PART
    suspend fun parseInScope() {
        val filteredElements: MutableList<Element> = mutableListOf()
        val doc: Document = withContext(Dispatchers.IO) { Jsoup.connect(URL).get() }

        //Clearing duplicates of elements
        for (currency in currencies) {
            val filterElement: Element? =
                doc.getElementsByAttributeValue("data-name", currency).first()
            if (filterElement != null) {
                filteredElements.add(filterElement)
            }
        }

        //Setting value to Map List
        for (element: Element in filteredElements) {
            val currency = element.attr("data-name").toString()
            val buy = element.attr("data-kup").toDouble()
            val sell = element.attr("data-pro").toDouble()
            buyList[currency] = buy
            sellList[currency] = sell
        }
        //Toasting
        withContext(Dispatchers.Main) {
            if (filteredElements.isNullOrEmpty()) {
                buyList.putAll(testBuyListInMap)
                sellList.putAll(testBuyListInMap)
                Toast.makeText(
                    application.applicationContext,
                    "ERROR: Server is offline",
                    Toast.LENGTH_SHORT
                ).show()
            } else
                Toast.makeText(
                    application.applicationContext,
                    "SUCCESS",
                    Toast.LENGTH_SHORT
                ).show()

        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    class ConvertViewModelFactory(private val application: Application) :
        ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(ConvertViewModel::class.java)) {
                ConvertViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

