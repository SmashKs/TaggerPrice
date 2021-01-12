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
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.devrapid.kotlinknifer.logw
import kotlinx.coroutines.launch
import taiwan.no.one.capture.databinding.FragmentCaptureBinding
import taiwan.no.one.capture.presentation.camera.BitmapAnalyzer
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CaptureFragment : BaseFragment<BaseActivity<*>, FragmentCaptureBinding>() {
    companion object {
        private const val TAG = "MyCapture"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private val vm by viewModel<CaptureViewModel>()
    private lateinit var cameraExecutor: ExecutorService
    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private val bitmapAnalyzer = BitmapAnalyzer { bitmap ->
        lifecycleScope.launch {
            vm.getOcrResult(bitmap)
            binding.ivPreview.setImageBitmap(bitmap)
        }
    }

    override fun bindLiveData() {
        vm.ocrResult.observe(this) {
            logw(it)
        }
    }

    override fun componentListenersBinding() {
        super.componentListenersBinding()
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun viewComponentBinding() {
        super.viewComponentBinding()
        permissionRequester.launch(Manifest.permission.CAMERA)
    }

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                // Take the user to the success fragment when permission is granted.
                initCamera()
            }
            else {
                Toast.makeText(parent, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun initCamera() {
        // Wait for the views to be properly laid out.
        binding.previewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = binding.previewFinder.display.displayId
            // Build UI controls
            //            updateCameraUi()
            // Bind use cases
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener({
                                                 // CameraProvider
                                                 cameraProvider = cameraProviderFuture.get()
                                                 // Select lenFacing depending on the available cameras
                                                 lensFacing = CameraSelector.LENS_FACING_BACK
                                                 // TODO: Enable or disable switching between cameras
                                                 bindCameraUseCases()
                                             }, ContextCompat.getMainExecutor(requireContext()))
        }
    }

    private fun bindCameraUseCases() {
        val metrics = getResolution()
        Log.d(TAG, "Screen metrics: ${metrics.width} x ${metrics.height}")

        val screenAspectRatio = aspectRatio(metrics.width, metrics.height)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = Surface.ROTATION_0
        // CameraProvider
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        // Preview
        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also { it.setSurfaceProvider(binding.previewFinder.surfaceProvider) }
        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also { it.setAnalyzer(cameraExecutor, bitmapAnalyzer) }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
        }
        catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed $exc")
        }
    }

    private fun getResolution(): Size {
        val displayMetrics = DisplayMetrics()
        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return Size(width, height)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}
