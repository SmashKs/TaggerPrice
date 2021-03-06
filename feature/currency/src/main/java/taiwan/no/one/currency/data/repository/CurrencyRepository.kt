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

package taiwan.no.one.currency.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import taiwan.no.one.core.data.cache.LayerCaching
import taiwan.no.one.core.data.cache.convertToKey
import taiwan.no.one.currency.data.contract.DataStore
import taiwan.no.one.currency.data.data.ConvertRateData
import taiwan.no.one.currency.data.data.CountryData
import taiwan.no.one.currency.data.data.CurrencyData
import taiwan.no.one.currency.domain.repostory.CurrencyRepo
import java.util.Date

internal class CurrencyRepository(
    private val local: DataStore,
    private val remote: DataStore,
    private val sp: SharedPreferences,
) : CurrencyRepo {
    companion object Constant {
        const val EXPIRED_DURATION = 1_800_000 // 30 * 60 * 1000 = half hour
    }

    override suspend fun fetchCurrencyRate(currencyKeys: List<Pair<String, String>>) =
        object : LayerCaching<List<ConvertRateData>>() {
            override var timestamp: Long
                get() = sp.getLong(convertToKey(currencyKeys.toString()), 0L)
                set(value) {
                    sp.edit { putLong(currencyKeys.toString(), value) }
                }

            override suspend fun saveCallResult(data: List<ConvertRateData>) {
                data.forEachIndexed { index, rate ->
                    local.createRateCurrencies(currencyKeys[index], rate)
                }
            }

            override suspend fun shouldFetch(data: List<ConvertRateData>) =
                Date().time - timestamp > EXPIRED_DURATION

            override suspend fun loadFromLocal() = local.retrieveRateCurrencies(currencyKeys)

            override suspend fun createCall() = remote.retrieveRateCurrencies(currencyKeys)
        }.value().map(ConvertRateData::convert)

    override suspend fun fetchCountries() = object : LayerCaching<List<CountryData>>() {
        override suspend fun saveCallResult(data: List<CountryData>) {
            local.createCountries(data)
        }

        override suspend fun shouldFetch(data: List<CountryData>) = data.isEmpty()

        override suspend fun loadFromLocal() = local.retrieveCountries()

        override suspend fun createCall() = remote.retrieveCountries()
    }.value().map(CountryData::convert)

    override suspend fun fetchCurrencies() = remote.retrieveCurrencies().map(CurrencyData::convert)
}
