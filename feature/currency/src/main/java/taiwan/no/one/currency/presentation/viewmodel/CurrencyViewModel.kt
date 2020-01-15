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

package taiwan.no.one.currency.presentation.viewmodel

import taiwan.no.one.core.presentation.viewmodel.BehindViewModel
import taiwan.no.one.core.presentation.viewmodel.ResultLiveData
import taiwan.no.one.currency.domain.model.CountryModel
import taiwan.no.one.currency.domain.model.CurrencyRateModel
import taiwan.no.one.currency.domain.parameter.RateRequestParams
import taiwan.no.one.currency.domain.usecase.FetchCountriesCase
import taiwan.no.one.currency.domain.usecase.FetchRateCase
import taiwan.no.one.ktx.livedata.toLiveData

class CurrencyViewModel(
    private val fetchRateCase: FetchRateCase,
    private val fetchCountriesCase: FetchCountriesCase
) : BehindViewModel() {
    private val _countries by lazy { ResultLiveData<List<CountryModel>>() }
    val countries = _countries.toLiveData()
    private val _rate by lazy { ResultLiveData<List<CurrencyRateModel>>() }
    val rate = _rate.toLiveData()

    fun getCountries() = launchBehind {
        _countries.postValue(fetchCountriesCase.execute())
    }

    fun getRate() = launchBehind {
        _rate.postValue(fetchRateCase.execute(RateRequestParams(listOf("TWD" to "USD"))))
    }
}
