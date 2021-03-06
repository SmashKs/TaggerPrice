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

package taiwan.no.one.currency.data.store

import com.google.gson.Gson
import taiwan.no.one.currency.data.contract.DataStore
import taiwan.no.one.currency.data.data.ConvertRateData
import taiwan.no.one.currency.data.data.CountryData
import taiwan.no.one.currency.data.data.CurrencyData
import taiwan.no.one.currency.data.remote.config.CurrencyRetrofitConfig
import taiwan.no.one.currency.data.remote.service.CurrencyConvertService
import taiwan.no.one.ext.exceptions.UnsupportedOperation

internal class RemoteStore(
    private val currencyConvertService: CurrencyConvertService,
    private val gson: Gson,
) : DataStore {
    override suspend fun retrieveRateCurrencies(currencyKeys: List<Pair<String, String>>): List<ConvertRateData> {
        val params = CurrencyRetrofitConfig.QUERY_PARAMS
        val query = currencyKeys.joinToString(",") { (from, to) -> "${from}_$to" }
        params.putAll(mapOf("q" to query, "compact" to "ultra"))
        val rates = currencyConvertService.getRateCurrencies(params)
        return rates.entrySet()
            .asSequence()
            .map { ConvertRateData(it.key, gson.fromJson(it.value, Double::class.java)) }
            .toList()
    }

    override suspend fun createRateCurrencies(pair: Pair<String, String>, currency: ConvertRateData) =
        throw UnsupportedOperation()

    override suspend fun retrieveCountries(): List<CountryData> {
        val params = CurrencyRetrofitConfig.QUERY_PARAMS
        val countries = currencyConvertService.getCountries(params).results ?: throw NullPointerException()
        return countries.entrySet()
            .asSequence()
            .map { gson.fromJson(it.value, CountryData::class.java) }
            .toList()
    }

    override suspend fun createCountries(countries: List<CountryData>) = throw UnsupportedOperation()

    override suspend fun retrieveCurrencies(): List<CurrencyData> {
        val params = CurrencyRetrofitConfig.QUERY_PARAMS
        val currencies = currencyConvertService.getCurrencies(params).results ?: throw NullPointerException()
        return currencies.entrySet()
            .asSequence()
            .map { gson.fromJson(it.value, CurrencyData::class.java) }
            .toList()
    }
}
