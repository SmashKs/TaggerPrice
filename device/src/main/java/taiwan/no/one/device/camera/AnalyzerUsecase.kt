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
import androidx.camera.core.ImageAnalysisConfig
import java.util.concurrent.Executor

class AnalyzerUsecase {
    companion object Builder {
        fun build(executor: Executor): ImageAnalysis {
            val config = AnalyzerUsecase().buildImageCaptureConfig()
            return ImageAnalysis(config).apply {
                setAnalyzer(executor, LuminosityAnalyzer())
            }
        }
    }

    // Setup image analysis pipeline that computes average pixel luminance
    private fun buildImageCaptureConfig() = ImageAnalysisConfig.Builder()
        // In our analysis, we care more about the latest image than
        // analyzing *every* image
        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        .build()
}
