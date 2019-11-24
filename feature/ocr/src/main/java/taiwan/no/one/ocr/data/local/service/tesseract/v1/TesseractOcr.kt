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

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import taiwan.no.one.ocr.data.local.service.OcrService
import java.io.File

internal class TesseractOcr(
    private val tess: TessBaseAPI
) : OcrService {
    override suspend fun recognize(bitmap: Bitmap): String {
        tess.setImage(bitmap)
        return tess.utF8Text
    }

    override suspend fun recognize(file: File): String {
        tess.setImage(file)
        return tess.utF8Text
    }

    override suspend fun recognize(raw: ByteArray): String {
        tess.setImage(raw, 100, 100, 100, 100)
        return tess.utF8Text
    }
}
