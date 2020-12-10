/*
 * MIT License
 *
 * Copyright (c) 2019 SmashKs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package taiwan.no.one.capture.presentation.viewmodel

import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import taiwan.no.one.core.presentation.viewmodel.BehindViewModel
import taiwan.no.one.ktx.livedata.toLiveData
import taiwan.no.one.taggerprice.entity.RateEntity
import taiwan.no.one.taggerprice.provider.CurrencyMethodProvider
import taiwan.no.one.taggerprice.provider.OCRMethodProvider

internal class CaptureViewModel(
    private val currencyProvider: CurrencyMethodProvider,
    private val ocrProvider: OCRMethodProvider,
) : BehindViewModel() {
    companion object {
        private const val INTERVAL = 1000  // 1 sec
    }

    private var lastProcessedTime = 0L
    private val _result by lazy { MutableLiveData<List<String>>() }
    val ocrResult = _result.toLiveData()
    private val _currency by lazy { MutableLiveData<RateEntity>() }
    val currency = _currency.toLiveData()
    private val _currencies by lazy { MutableLiveData<List<RateEntity>>() }
    val currencies = _currencies.toLiveData()
    val countries = liveData { emit(currencyProvider.getCountries()) }

    @UiThread
    fun getCurrency(from: String, to: String) = viewModelScope.launch {
        _currency.value = currencyProvider.getRate(from, to)
    }

    @UiThread
    fun getCurrencies(pairs: List<Pair<String, String>>) = viewModelScope.launch {
        _currencies.value = currencyProvider.getRates(*pairs.toTypedArray())
    }

    @UiThread
    fun getOcrResult(bitmap: Bitmap) = viewModelScope.launch {
        val time = System.currentTimeMillis()
        if (time - lastProcessedTime > INTERVAL) {
            lastProcessedTime = time
            _result.value = ocrProvider.getOCRResult(bitmap)
        }
    }
}
