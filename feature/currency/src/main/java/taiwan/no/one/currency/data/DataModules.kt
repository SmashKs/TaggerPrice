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

package taiwan.no.one.currency.data

import android.content.Context
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import retrofit2.Retrofit
import taiwan.no.one.core.data.remote.RetrofitConfig
import taiwan.no.one.core.data.remote.provider.OkHttpClientProvider
import taiwan.no.one.core.data.remote.provider.RetrofitProvider
import taiwan.no.one.currency.FeatModules.FEAT_NAME
import taiwan.no.one.currency.data.remote.config.CurrencyRetrofitConfig
import taiwan.no.one.currency.data.remote.service.CurrencyConvertService
import taiwan.no.one.currency.data.repository.CurrencyRepository
import taiwan.no.one.currency.data.store.LocalStore
import taiwan.no.one.currency.data.store.RemoteStore
import taiwan.no.one.currency.domain.repostory.CurrencyRepo
import taiwan.no.one.taggerprice.TaggerPriceApp
import taiwan.no.one.taggerprice.provider.ModuleProvider

internal object DataModules : ModuleProvider {
    override fun provide() = Kodein.Module("${FEAT_NAME}DataModule") {
        import(localProvide())
        import(remoteProvide(TaggerPriceApp.appContext))

        bind<LocalStore>() with singleton { LocalStore() }
        bind<RemoteStore>() with singleton { RemoteStore(instance(), instance()) }

        bind<CurrencyRepo>() with singleton { CurrencyRepository(instance()) }
    }

    private fun localProvide() = Kodein.Module("${FEAT_NAME}LocalModule") {
    }

    private fun remoteProvide(context: Context) = Kodein.Module("${FEAT_NAME}RemoteModule") {
        bind<RetrofitConfig>() with singleton {
            CurrencyRetrofitConfig(context, OkHttpClientProvider(context), RetrofitProvider())
        }
        bind<Retrofit>() with singleton {
            instance<RetrofitConfig>().provideRetrofitBuilder().build()
        }
        bind<CurrencyConvertService>() with singleton {
            instance<Retrofit>().create(CurrencyConvertService::class.java)
        }
    }
}
