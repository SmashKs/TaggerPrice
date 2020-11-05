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

package taiwan.no.one.currency

import com.google.auto.service.AutoService
import org.kodein.di.DIAware
import org.kodein.di.instance
import taiwan.no.one.currency.domain.model.CountryModel
import taiwan.no.one.currency.domain.toEntity
import taiwan.no.one.currency.domain.usecase.FetchCountriesCase
import taiwan.no.one.currency.domain.usecase.FetchRateCase
import taiwan.no.one.taggerprice.TaggerPriceApp
import taiwan.no.one.taggerprice.provider.CurrencyMethodProvider

@AutoService(CurrencyMethodProvider::class)
class MethodProvider : CurrencyMethodProvider, DIAware {
    override val di by lazy { (TaggerPriceApp.appContext as DIAware).di }
    private val fetchRateCase by instance<FetchRateCase>()
    private val fetchCountriesCase by instance<FetchCountriesCase>()

    override suspend fun getCountries() =
        (fetchCountriesCase.execute().getOrNull() ?: throw Exception()).map(CountryModel::toEntity)
}
