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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.display.DisplayManager
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.devrapid.kotlinknifer.logd
import com.devrapid.kotlinknifer.loge
import com.devrapid.kotlinknifer.logw
import com.devrapid.kotlinshaver.ui
import taiwan.no.one.capture.databinding.FragmentCaptureBinding
import taiwan.no.one.capture.presentation.viewmodel.CaptureViewModel
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.device.camera.LuminosityAnalyzer
import java.io.ByteArrayOutputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias BitmapListener = (bitmap: Bitmap) -> Unit

class CaptureFragment : BaseFragment<BaseActivity<*>, FragmentCaptureBinding>() {

    private lateinit var cameraExecutor: ExecutorService

    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    override fun componentListenersBinding() {
        super.componentListenersBinding()

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun viewComponentBinding() {
        super.viewComponentBinding()
        permissionRequester.launch(Manifest.permission.CAMERA)
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
            cameraProviderFuture.addListener(Runnable {

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

        preview?.setSurfaceProvider(binding.previewFinder.surfaceProvider)

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
            .also {
                it.setAnalyzer(cameraExecutor, BitmapAnalyzer { bitmap ->
                    Log.i(TAG, "width: ${bitmap.width}, height: ${bitmap.height}")
                })
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
        } catch (exc: Exception) {
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

    private class BitmapAnalyzer(listener: BitmapListener? = null): ImageAnalysis.Analyzer {

        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set
        private val bitmapListener = ArrayList<BitmapListener>().apply { listener?.let { add(it) } }

        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (bitmapListener.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)

            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                                     frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0

            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases

            lastAnalyzedTimestamp = frameTimestamps.first

            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer

            // Extract image data from callback object
            val data = buffer.toByteArray()

            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }

            // Compute average luminance for the image
            val luma = pixels.average()

            // Call all listeners with new value
            bitmapListener.forEach { it(getBitmap(data, image.width, image.height)) }

            image.close()
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        private fun getBitmap(rawImage: ByteArray, width: Int, height: Int): Bitmap {
            val image = YuvImage(rawImage, ImageFormat.NV21, width, height, null)
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, width, height), 80, stream)
            return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
        }
    }

    companion object {
        private const val TAG = "MyCapture"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}

/*
class CaptureFragment : BaseFragment<BaseActivity<*>, FragmentCaptureBinding>() {
    //region Variables
    private var displayId = -1
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    // Blocking camera operations are performed using this executor.
    private lateinit var cameraExecutor: ExecutorService
    private val vm by viewModel<CaptureViewModel>()
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
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

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CaptureFragment.displayId) {
                logd("Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }
    //endregion

    //region Fragment LifeCycle
    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()
        // Unregister the broadcast receivers and listeners
        displayManager.unregisterDisplayListener(displayListener)
    }
    //endregion

    //region Customized methods
    override fun viewComponentBinding() {
        super.viewComponentBinding()
        permissionRequester.launch(Manifest.permission.CAMERA)
    }

    override fun componentListenersBinding() {
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
    }
    //endregion

    private fun initCamera() {
        // Wait for the views to be properly laid out.
        binding.previewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = binding.previewFinder.display.displayId
            // Build UI controls
            //            updateCameraUi()
            // Bind use cases
            bindCameraUseCases()
        }
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.previewFinder.display.getRealMetrics(it) }
        logd("Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        logd("Preview aspect ratio: $screenAspectRatio")

        val rotation = binding.previewFinder.display.rotation

        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            val cameraProvider = cameraProviderFuture.get()

            // *** Preview
            preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.previewFinder.surfaceProvider)

            // *** ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits requested capture mode
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            // *** ImageAnalysis
            imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                // The analyzer can then be assigned to the instance
                .also { it ->
                    // TODO(Jieyi): 4/9/2019 For testing the [YuvToRgbConverter].
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        // Values returned from our analyzer are passed to the attached listener
//                        // We log image analysis results here - you should do something useful instead!
//                        logd("Average luminosity: $luma")
//                        val yuvImage = YuvImage(luma,
//                                                ImageFormat.NV21,
//                                                binding.clContainer.width,
//                                                binding.clContainer.height,
//                                                null)
//                        io {
//                            val stream = ByteArrayOutputStream()
//                            yuvImage.compressToJpeg(Rect(0, 0, binding.clContainer.width, binding.clContainer.height),
//                                                    80,
//                                                    stream)
//                            val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
//                            ui { binding.ivPreview.setImageBitmap(bmp) }
//                            stream.close()
//                        }
//                    })
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                        val buffer = image.image?.planes?.get(0)?.buffer
                        val bytes = ByteArray(buffer?.capacity() ?: 0)
                        buffer?.get(bytes)
                        logw(bytes.size)
//                        YuvToRgbConverter(requireContext()).yuvToRgb(image, bmp)
                        ui {
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                            binding.ivPreview.setImageBitmap(bmp)
                        }
                        image.close()
                    })
                }

            // Must unbind the use-cases before rebinding them.
            cameraProvider.unbindAll()

            try {
                // A variable number of use-cases can be passed here -
                // camera provides access to CameraControl & CameraInfo
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            }
            catch (exc: Exception) {
                loge("Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(parent))
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently, it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        if (abs(previewRatio - LuminosityAnalyzer.RATIO_4_3_VALUE) <=
            abs(previewRatio - LuminosityAnalyzer.RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
//    private fun updateCameraUi() {
//
//        // Remove previous UI if any
//        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
//            container.removeView(it)
//        }
//
//        // Inflate a new view containing all UI for controlling the camera
//        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)
//
//        // In the background, load latest photo taken (if any) for gallery thumbnail
//        lifecycleScope.launch(Dispatchers.IO) {
//            outputDirectory.listFiles { file ->
//                EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT))
//            }?.max()?.let {
//                setGalleryThumbnail(Uri.fromFile(it))
//            }
//        }
//
//        // Listener for button used to capture photo
//        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
//
//            // Get a stable reference of the modifiable image capture use case
//            imageCapture?.let { imageCapture ->
//
//                // Create output file to hold the image
//                val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)
//
//                // Setup image capture metadata
//                val metadata = Metadata().apply {
//
//                    // Mirror image when using the front camera
//                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
//                }
//
//                // Create output options object which contains file + metadata
//                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
//                    .setMetadata(metadata)
//                    .build()
//
//                // Setup image capture listener which is triggered after photo has been taken
//                imageCapture.takePicture(
//                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
//                    override fun onError(exc: ImageCaptureException) {
//                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                    }
//
//                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
//                        Log.d(TAG, "Photo capture succeeded: $savedUri")
//
//                        // We can only change the foreground Drawable using API level 23+ API
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                            // Update the gallery thumbnail with latest picture taken
//                            setGalleryThumbnail(savedUri)
//                        }
//
//                        // Implicit broadcasts will be ignored for devices running API level >= 24
//                        // so if you only target API level 24+ you can remove this statement
//                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
//                            requireActivity().sendBroadcast(
//                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
//                            )
//                        }
//
//                        // If the folder selected is an external media directory, this is
//                        // unnecessary but otherwise other apps will not be able to access our
//                        // images unless we scan them using [MediaScannerConnection]
//                        val mimeType = MimeTypeMap.getSingleton()
//                            .getMimeTypeFromExtension(savedUri.toFile().extension)
//                        MediaScannerConnection.scanFile(
//                            context,
//                            arrayOf(savedUri.toString()),
//                            arrayOf(mimeType)
//                        ) { _, uri ->
//                            Log.d(TAG, "Image capture scanned into media store: $uri")
//                        }
//                    }
//                })
//
//                // We can only change the foreground Drawable using API level 23+ API
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//                    // Display flash animation to indicate that photo was captured
//                    container.postDelayed({
//                                              container.foreground = ColorDrawable(Color.WHITE)
//                                              container.postDelayed(
//                                                  { container.foreground = null }, ANIMATION_FAST_MILLIS)
//                                          }, ANIMATION_SLOW_MILLIS)
//                }
//            }
//        }
//
//        // Listener for button used to switch cameras
//        controls.findViewById<ImageButton>(R.id.camera_switch_button).setOnClickListener {
//            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
//                CameraSelector.LENS_FACING_BACK
//            }
//            else {
//                CameraSelector.LENS_FACING_FRONT
//            }
//            // Re-bind use cases to update selected camera
//            bindCameraUseCases()
//        }
//
//        // Listener for button used to view the most recent photo
//        controls.findViewById<ImageButton>(R.id.photo_view_button).setOnClickListener {
//            // Only navigate when the gallery has photos
//            if (true == outputDirectory.listFiles()?.isNotEmpty()) {
//                Navigation.findNavController(
//                    requireActivity(), R.id.fragment_container
//                ).navigate(CameraFragmentDirections
//                               .actionCameraToGallery(outputDirectory.absolutePath))
//            }
//        }
//    }
}
*/
