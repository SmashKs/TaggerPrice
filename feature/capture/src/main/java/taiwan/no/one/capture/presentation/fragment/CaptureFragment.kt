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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.fragment_capture.viewFinder
import taiwan.no.one.capture.databinding.FragmentCaptureBinding
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.device.camera.LuminosityAnalyzer
import taiwan.no.one.device.util.AutoFitPreviewBuilder
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
            ActivityCompat.requestPermissions(parent, requiredPermissions, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun requestCameraIfFail(onFailure: (() -> Unit)?) {
        if (requireContext().allPermissionsGranted(requiredPermissions)) {
            viewFinder.post { startCamera() }
        }
        else {
            onFailure?.invoke()
        }
    }

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()
        // Build the viewfinder use case
        val preview = AutoFitPreviewBuilder.build(previewConfig, viewFinder)

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(parent.mainExecutor, LuminosityAnalyzer())
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
    }
}
