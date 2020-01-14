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

package taiwan.no.one.ocr.data.local.service.firebase.v1

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import kotlinx.coroutines.delay
import taiwan.no.one.ocr.data.local.service.OcrService
import java.io.File

internal class FirebaseOcr(
    private val context: Context,
    private val recognizer: FirebaseVisionTextRecognizer
) : OcrService {
    companion object Constant {
        private const val BREAK_TIME_FOR_WHILE = 100L
    }

    override suspend fun recognize(bitmap: Bitmap): String {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        return process(image)
    }

    override suspend fun recognize(file: File): String {
        val image = try {
            FirebaseVisionImage.fromFilePath(context, file.toUri())
        }
        catch (e: Exception) {
            throw Exception()
        }
        return process(image)
    }

    override suspend fun recognize(raw: ByteArray, lang: String): String {
        val metadata = FirebaseVisionImageMetadata.Builder().build()
        val image = FirebaseVisionImage.fromByteArray(raw, metadata)
        return process(image)
    }

    private suspend fun process(image: FirebaseVisionImage): String {
        val task = recognizer.processImage(image)
        // Using while-loop for polling because recognizing will take time.
        while (!task.isComplete) {
            delay(BREAK_TIME_FOR_WHILE)
        }
        return if (task.isSuccessful) {
            task.result?.text.orEmpty()
        }
        else {
            throw task.exception ?: Exception()
        }
    }
}
