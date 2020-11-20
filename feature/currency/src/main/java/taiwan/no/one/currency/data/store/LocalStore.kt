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

import taiwan.no.one.core.data.cache.Caching
import taiwan.no.one.currency.data.contract.DataStore
import taiwan.no.one.currency.data.data.CountryData
import taiwan.no.one.currency.data.local.service.room.v1.CountryDao
import taiwan.no.one.ext.exceptions.UnsupportedOperation
import java.util.Calendar

internal class LocalStore(
    private val countryDao: CountryDao,
    private val mmkvCache: Caching,
) : DataStore {
    override suspend fun retrieveRateCurrencies(currencyKeys: List<Pair<String, String>>) =
        TODO("Using the MMKVCaching to achieve")

    override suspend fun retrieveCountries(): List<CountryData> {
        val oneMonthBeforeDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.time
        return countryDao.getCountries(oneMonthBeforeDate)
    }

    override suspend fun createCountries(countries: List<CountryData>) = tryWrapper {
        countryDao.insert(*countries.toTypedArray())
    }

    override suspend fun retrieveCurrencies() = throw UnsupportedOperation()
}
