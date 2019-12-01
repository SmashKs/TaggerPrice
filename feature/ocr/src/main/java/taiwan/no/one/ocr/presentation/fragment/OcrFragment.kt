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

package taiwan.no.one.ocr.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.devrapid.kotlinknifer.logw
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.ocr.databinding.FragmentOcrBinding
import taiwan.no.one.ocr.presentation.viewmodel.OcrViewModel

internal class OcrFragment : BaseFragment<BaseActivity<*>, FragmentOcrBinding>() {
    private val vm by viewModels<OcrViewModel> { vmFactory }

    override fun bindLiveData() {
        vm.result.observe(viewLifecycleOwner) {
            it.onSuccess {
                logw(it)
            }.onFailure {
                logw(it.localizedMessage.toString())
            }
        }
    }

    override fun rendered(savedInstanceState: Bundle?) {
        vm.getResult()
    }
}
