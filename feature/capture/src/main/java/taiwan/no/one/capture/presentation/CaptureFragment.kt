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

package taiwan.no.one.capture.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.ImageProxy
import androidx.camera.core.PreviewConfig
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devrapid.kotlinknifer.logw
import kotlinx.android.synthetic.main.fragment_capture.viewFinder
import org.kodein.di.description
import org.kodein.di.fullDescription
import taiwan.no.one.capture.databinding.FragmentCaptureBinding
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.device.util.AutoFitPreviewBuilder
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class CaptureFragment : BaseFragment<BaseActivity<*>, FragmentCaptureBinding>() {
    companion object {
        // This is an arbitrary number we are using to keep track of the permission
        // request. Where an app has multiple context for requesting permission,
        // this can help differentiate the different contexts.
        private val REQUEST_CODE_PERMISSIONS = 10

        // This is an array of all the permission specified in the manifest.
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private val vm by viewModel<CaptureViewModel>()

    override fun rendered(savedInstanceState: Bundle?) {
        super.rendered(savedInstanceState)
        println("=================================================")
        println(kodein.container.tree.bindings.description())
        println("=================================================")
        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        }
        else {
            ActivityCompat.requestPermissions(parent, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            }
            else {
                Toast.makeText(parent,
                               "Permissions not granted by the user.",
                               Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
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

    private class LuminosityAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L

        /**
         * Helper extension function used to extract a byte array from an
         * image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            val currentTimestamp = System.currentTimeMillis()
            // Calculate the average luma no more often than every second
            if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
                // Since format in ImageAnalysis is YUV, image.planes[0]
                // contains the Y (luminance) plane
                val buffer = image.planes[0].buffer
                // Extract image data from callback object
                val data = buffer.toByteArray()
                // Convert the data into an array of pixel values
                val pixels = data.map { it.toInt() and 0xFF }
                // Compute average luminance for the image
                val luma = pixels.average()
                // Log the new luma value
                Log.d("CameraXApp", "Average luminosity: $luma")
                // Update timestamp of last analyzed frame
                lastAnalyzedTimestamp = currentTimestamp
            }
        }
    }
}
