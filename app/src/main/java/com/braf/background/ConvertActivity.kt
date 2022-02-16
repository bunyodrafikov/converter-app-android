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

package com.braf.background

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.braf.background.databinding.ActivityConvertBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConvertActivity : AppCompatActivity() {

    private val viewModel: ConvertViewModel by viewModels {
        ConvertViewModel.ConvertViewModelFactory(
            application
        )
    }
    private lateinit var binding: ActivityConvertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConvertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Parsing and setting data from internet in lifecycleScope
        launchData()
        binding.calculate.setOnClickListener {
            val currency1 = binding.currency1.text.toString()
            val currency2 = binding.currency2.text.toString()
            val input = binding.inputValue1.text.toString()
            lifecycleScope.launch {
                viewModel.calculate(currency1, currency2, input)
                withContext(Dispatchers.Main) {
                    binding.inputValue2.setText(viewModel.calculate(currency1, currency2, input))
                }
            }
        }

        bind(binding)
    }

    private fun bind(binding: ActivityConvertBinding) {
        val allCurrencies = resources.getStringArray(R.array.currencies_array)
        val uri: Uri = "https://goo.gl/maps/BCpZcZPrJpzuRhiq5".toUri()

        binding.apply {
            lifecycleOwner = this@ConvertActivity
            currency1.setText(allCurrencies[0])
            currency2.setText(allCurrencies[1])
            restartAdapterForDropdown()
            replace.setOnClickListener {
                replace()
                restartAdapterForDropdown()
            }
            branches.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            refresh.setOnClickListener { launchData() }
        }
    }

    private fun replace() {
        val cur1 = binding.currency1.text
        val cur2 = binding.currency2.text
        val val1 = binding.inputValue1.text
        val val2 = binding.inputValue2.text
        binding.apply {
            currency1.text = cur2
            currency2.text = cur1
            inputValue1.text = val2
            inputValue2.text = val1
        }
    }

    private fun restartAdapterForDropdown() {
        val allCurrencies = resources.getStringArray(R.array.currencies_array)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, allCurrencies)
        binding.apply {
            currency1.setAdapter(arrayAdapter)
            currency2.setAdapter(arrayAdapter)
        }
    }

    private fun launchData() {
        lifecycleScope.launch {
            viewModel.parseInScope()
            withContext(Dispatchers.Main) {
                binding.usdBuy.text = viewModel.buyList["USD"].toString()
                binding.usdSell.text = viewModel.sellList["USD"].toString()
                binding.eurBuy.text = viewModel.buyList["EUR"].toString()
                binding.eurSell.text = viewModel.sellList["EUR"].toString()
                binding.rubBuy.text = viewModel.buyList["RUB"].toString()
                binding.rubSell.text = viewModel.sellList["RUB"].toString()

                binding.inputValue2.setText("")
                binding.inputValue1.setText("")
                restartAdapterForDropdown()
            }
        }
    }

}