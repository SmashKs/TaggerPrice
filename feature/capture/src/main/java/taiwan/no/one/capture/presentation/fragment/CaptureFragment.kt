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
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraX
import taiwan.no.one.capture.databinding.FragmentCaptureBinding
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.device.camera.AnalyzerUsecase
import taiwan.no.one.device.camera.ImageCaptureUsecase
import taiwan.no.one.device.camera.PreviewUsecase
import taiwan.no.one.ktx.context.allPermissionsGranted

class CaptureFragment : BaseFragment<BaseActivity<*>, FragmentCaptureBinding>() {
    companion object Constant {
        // This is an arbitrary number we are using to keep track of the permission
        // request. Where an app has multiple context for requesting permission,
        // this can help differentiate the different contexts.
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    // This is an array of all the permission specified in the manifest.
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val vm by viewModel<CaptureViewModel>()

    /**
     * Process result from permission request dialog box, has the request been granted?
     * If yes, start Camera. Otherwise, display a toast.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            requestCameraIfFail {
                Toast.makeText(parent,
                               "Permissions not granted by the user.",
                               Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun rendered(savedInstanceState: Bundle?) {
        super.rendered(savedInstanceState)
        // Request camera permissions
        requestCameraIfFail {
            requestPermissions(requiredPermissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun requestCameraIfFail(onFailure: (() -> Unit)?) {
        if (requireContext().allPermissionsGranted(requiredPermissions)) {
            binding.viewFinder.post { startCamera() }
        }
        else {
            onFailure?.invoke()
        }
    }

    private fun startCamera() {
        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this,
                                PreviewUsecase.build(Size(640, 480), binding.viewFinder),
                                ImageCaptureUsecase.build(),
                                AnalyzerUsecase.build(parent.mainExecutor))
    }
}
