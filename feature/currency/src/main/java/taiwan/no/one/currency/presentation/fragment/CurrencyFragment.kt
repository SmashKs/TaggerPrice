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

package taiwan.no.one.currency.presentation.fragment

import android.os.Bundle
import com.devrapid.kotlinknifer.logw
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.currency.databinding.FragmentCurrencyBinding
import taiwan.no.one.currency.presentation.viewmodel.CurrencyViewModel
import taiwan.no.one.taggerprice.presentation.activity.MainActivity

// NOTE(jieyi): 11/29/20 This is for testing only.
class CurrencyFragment : BaseFragment<MainActivity, FragmentCurrencyBinding>() {
    private val vm by viewModel<CurrencyViewModel>()

    override fun bindLiveData() {
        vm.countries.observe(this) {
            it.onSuccess {
                logw(it)
            }.onFailure(::logw)
        }
        vm.rate.observe(this) {
            it.onSuccess {
                logw(it)
            }.onFailure(::logw)
        }
    }

    override fun rendered(savedInstanceState: Bundle?) {
        vm.getRate()
    }
}
