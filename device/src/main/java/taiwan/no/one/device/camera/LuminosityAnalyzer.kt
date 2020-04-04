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

package taiwan.no.one.device.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Locale

/** Helper type alias used for analysis use case callbacks */
typealias LumaListener = (luma: Double) -> Unit
/** */
typealias RawDataListener = (rawData: ByteArray) -> Unit

/**
 * Our custom image analysis class.
 *
 * <p>All we need to do is override the function `analyze` with our desired operations. Here,
 * we compute the average luminosity of the image by looking at the Y plane of the YUV frame.
 */
class LuminosityAnalyzer(listener: RawDataListener? = null) : ImageAnalysis.Analyzer {
    companion object {
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis()) + extension)
    }

    var framesPerSecond = -1.0
        private set
    private val frameRateWindow = 8
    private val frameTimestamps = ArrayDeque<Long>(5)
    private val listeners = ArrayList<RawDataListener>().apply { listener?.let { add(it) } }
    private val lumaListeners = ArrayList<LumaListener>()
    private var lastAnalyzedTimestamp = 0L

    /**
     * Used to add listeners that will be called with raw data
     */
    fun onRawAnalyzed(listener: RawDataListener) = listeners.add(listener)

    /**
     * Used to add listeners that will be called with each luma computed
     */
    fun onFrameAnalyzed(listener: LumaListener) = lumaListeners.add(listener)

    /**
     * Helper extension function used to extract a byte array from an
     * image plane buffer
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind() // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data) // Copy the buffer into a byte array
        return data // Return the byte array
    }

    /**
     * Analyzes an image to produce a result.
     *
     * <p>The caller is responsible for ensuring this analysis method can be executed quickly
     * enough to prevent stalls in the image acquisition pipeline. Otherwise, newly available
     * images will not be acquired and analyzed.
     *
     * <p>The image passed to this method becomes invalid after this method returns. The caller
     * should not store external references to this image, as these references will become
     * invalid.
     *
     * @param image image being analyzed VERY IMPORTANT: Analyzer method implementation must
     * call image.close() on received images when finished using them. Otherwise, new images
     * may not be received or the camera may stall, depending on back pressure setting.
     */
    override fun analyze(image: ImageProxy) {
        // If there are no listeners attached, we don't need to perform analysis
//        if (lumaListeners.isEmpty()) {
//            image.close()
//            return
//        }

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
        val buffer = image.planes[1].buffer
        // Extract image data from callback object
        val data = buffer.toByteArray()
        listeners.forEach { it(data) }
        // Convert the data into an array of pixel values ranging 0-255
        val pixels = data.map { it.toInt() and 0xFF }
        // Compute average luminance for the image
        val luma = pixels.average()
        // Call all listeners with new value
        lumaListeners.forEach { it(luma) }
        image.close()
    }
}
