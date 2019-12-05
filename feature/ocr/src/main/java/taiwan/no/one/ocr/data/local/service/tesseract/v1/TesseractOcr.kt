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

package taiwan.no.one.ocr.data.local.service.tesseract.v1

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import com.googlecode.tesseract.android.TessBaseAPI
import taiwan.no.one.ext.DEFAULT_STR
import taiwan.no.one.ocr.data.local.service.OcrService
import java.io.File
import java.io.FileOutputStream

internal class TesseractOcr(
    private val tess: TessBaseAPI
) : OcrService {
    companion object {
        private val DATA_PATH: String = "${Environment.getExternalStorageDirectory().absolutePath}${File.separator}"
        private val TESSDATA_PATH = "${DATA_PATH}tessdata"
    }

    private var currentLang = DEFAULT_STR

    override suspend fun recognize(context: Context, bitmap: Bitmap): String {
        tess.setImage(bitmap)
        return tess.utF8Text
    }

    override suspend fun recognize(context: Context, file: File): String {
        tess.setImage(file)
        return tess.utF8Text
    }

    override suspend fun recognize(context: Context, raw: ByteArray, lang: String): String {
        if (shouldCopyTo(getLanguagePath(lang))) {
            copyToSD(context, getLanguagePath(lang), getTrainedDataLangName(lang))
        }
        if (lang != currentLang) {
            currentLang = lang
            tess.init(DATA_PATH, lang)
        }
        tess.setImage(raw, 100, 100, 100, 100)
        return tess.utF8Text
    }

    private fun shouldCopyTo(path: String) = File(path).exists()

    private fun copyToSD(context: Context, path: String, name: String) {
        // Create the dirs and file
        File(path).apply {
            if (!exists()) {
                File(parent.orEmpty()).let {
                    if (it.exists()) return@let
                    // File path is `DataPath/tessdata/xxx.traineddata`, if .../tessdata/... doesn't exist,
                    // it should be created before copy.
                    it.mkdirs()
                }
                createNewFile()
            }
        }
        // Copy the data
        context.applicationContext.assets.open(name).use { `is` ->
            FileOutputStream(File(path)).use { os ->
                val bytes = ByteArray(2048)
                var len = 0
                while (`is`.read(bytes).also { len = it } != -1) {
                    os.write(bytes, 0, len)
                }
                os.flush()
            }
        }
    }

    private fun getTrainedDataLangName(lang: String) = "$lang.traineddata"

    private fun getLanguagePath(lang: String) = "$TESSDATA_PATH${File.separator}${getTrainedDataLangName(lang)}"
}
