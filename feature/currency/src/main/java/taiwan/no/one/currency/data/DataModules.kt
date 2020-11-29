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
import android.content.SharedPreferences
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import retrofit2.Retrofit
import taiwan.no.one.core.data.cache.Caching
import taiwan.no.one.core.data.cache.MmkvCache
import taiwan.no.one.core.data.remote.RetrofitConfig
import taiwan.no.one.core.data.remote.provider.OkHttpClientProvider
import taiwan.no.one.core.data.remote.provider.RetrofitProvider
import taiwan.no.one.currency.FeatModules.Companion.FEAT_NAME
import taiwan.no.one.currency.data.contract.DataStore
import taiwan.no.one.currency.data.local.config.CurrencyDatabase
import taiwan.no.one.currency.data.local.service.room.v1.CountryDao
import taiwan.no.one.currency.data.remote.config.CurrencyRetrofitConfig
import taiwan.no.one.currency.data.remote.service.CurrencyConvertService
import taiwan.no.one.currency.data.repository.CurrencyRepository
import taiwan.no.one.currency.data.store.LocalStore
import taiwan.no.one.currency.data.store.RemoteStore
import taiwan.no.one.currency.domain.repostory.CurrencyRepo
import taiwan.no.one.taggerprice.provider.ModuleProvider

internal object DataModules : ModuleProvider {
    private const val TAG_CURRENCY_RETROFIT = "currency retrofit"
    private const val TAG_LOCAL_DATA_STORE = "local data store"
    private const val TAG_REMOTE_DATA_STORE = "remote data store"
    private const val TAG_SP_TIMESTAMP = "tag of the time stamp sharedpreferenced"

    override fun provide(context: Context) = DI.Module("${FEAT_NAME}DataModule") {
        import(localProvide())
        import(remoteProvide(context))

        bind<DataStore>(TAG_LOCAL_DATA_STORE) with singleton { LocalStore(instance(), instance()) }
        bind<DataStore>(TAG_REMOTE_DATA_STORE) with singleton { RemoteStore(instance(), instance()) }
        bind<SharedPreferences>(TAG_SP_TIMESTAMP) with singleton {
            context.getSharedPreferences(TAG_SP_TIMESTAMP, Context.MODE_PRIVATE)
        }
        bind<CurrencyRepo>() with singleton {
            CurrencyRepository(instance(TAG_LOCAL_DATA_STORE),
                               instance(TAG_REMOTE_DATA_STORE),
                               instance(TAG_SP_TIMESTAMP))
        }
    }

    private fun localProvide() = DI.Module("${FEAT_NAME}LocalModule") {
        bind<CurrencyDatabase>() with singleton { CurrencyDatabase.getDatabase(instance()) }
        bind<CountryDao>() with singleton { instance<CurrencyDatabase>().createCountryDao() }

        bind<Caching>() with singleton { MmkvCache(instance()) }
    }

    private fun remoteProvide(context: Context) = DI.Module("${FEAT_NAME}RemoteModule") {
        bind<RetrofitConfig>() with singleton {
            CurrencyRetrofitConfig(context, OkHttpClientProvider(context), RetrofitProvider())
        }
        bind<Retrofit>(TAG_CURRENCY_RETROFIT) with singleton {
            instance<RetrofitConfig>().provideRetrofitBuilder().build()
        }
        bind<CurrencyConvertService>() with singleton {
            instance<Retrofit>(TAG_CURRENCY_RETROFIT).create(CurrencyConvertService::class.java)
        }
    }
}
