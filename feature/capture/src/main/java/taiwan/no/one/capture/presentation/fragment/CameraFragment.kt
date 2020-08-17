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

package taiwan.no.one.capture.presentation.fragment

import android.Manifest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import taiwan.no.one.capture.databinding.FragmentCameraBinding
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment

class CameraFragment : BaseFragment<BaseActivity<*>, FragmentCameraBinding>() {
    //region Variables
    // Blocking camera operations are performed using this executor.
    private val vm by viewModel<CaptureViewModel>()
    private val permissionRequester = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.all { it.value }) {
            // Take the user to the success fragment when permission is granted.
            binding.cvCamera.apply {
                setLifecycleOwner(viewLifecycleOwner)
                addFrameProcessor {}
            }
        }
        else {
            Toast.makeText(parent, "Permission request denied", Toast.LENGTH_LONG).show()
        }
    }
    //endregion

    //region Customized methods
    override fun viewComponentBinding() {
        super.viewComponentBinding()
        permissionRequester.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.RECORD_AUDIO))
    }
    //endregion
}
