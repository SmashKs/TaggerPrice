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

package taiwan.no.one.ocr.data

import android.content.Context
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.googlecode.tesseract.android.TessBaseAPI
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import taiwan.no.one.ocr.FeatModules.Companion.FEAT_NAME
import taiwan.no.one.ocr.data.local.service.OcrService
import taiwan.no.one.ocr.data.local.service.firebase.v1.FirebaseOcr
import taiwan.no.one.ocr.data.local.service.tesseract.v1.TesseractOcr
import taiwan.no.one.ocr.data.remote.service.FirebaseMLService
import taiwan.no.one.ocr.data.repository.OcrRepository
import taiwan.no.one.ocr.data.store.LocalStore
import taiwan.no.one.ocr.data.store.RemoteStore
import taiwan.no.one.ocr.domain.repository.OcrRepo
import taiwan.no.one.taggerprice.TaggerPriceApp
import taiwan.no.one.taggerprice.provider.ModuleProvider
import taiwan.no.one.ocr.data.remote.service.firebase.v1.FirebaseOcr as RemoteFirebaseOcr

internal object DataModules : ModuleProvider {
    private const val TAG_LOCAL_SERVICE = "local service"
    private const val TAG_REMOTE_SERVICE = "remote service"

    private const val TAG_FIREBASE = "firebase"
    private const val TAG_TESSERACT = "tesseract"

    override fun provide(context: Context) = DI.Module("${FEAT_NAME}DataModule") {
        import(localProvide(TaggerPriceApp.appContext.applicationContext))
        import(remoteProvide())

        bind<LocalStore>() with singleton { LocalStore(instance(TAG_TESSERACT), instance(TAG_FIREBASE)) }
        bind<RemoteStore>() with singleton { RemoteStore() }

        bind<OcrRepo>() with singleton { OcrRepository(instance(), instance()) }
    }

    private fun localProvide(context: Context) = DI.Module("${FEAT_NAME}LocalModule") {
        bind<TessBaseAPI>() with singleton {
            TessBaseAPI().apply {
                pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
            }
        }
        bind<FirebaseVisionTextRecognizer>(TAG_LOCAL_SERVICE) with singleton {
            FirebaseVision.getInstance().onDeviceTextRecognizer
        }

        bind<OcrService>(TAG_TESSERACT) with singleton { TesseractOcr(context, instance()) }
        bind<OcrService>(TAG_FIREBASE) with singleton { FirebaseOcr(context, instance(TAG_LOCAL_SERVICE)) }
    }

    private fun remoteProvide() = DI.Module("${FEAT_NAME}RemoteModule") {
        bind<FirebaseVisionTextRecognizer>(TAG_REMOTE_SERVICE) with singleton {
            FirebaseVision.getInstance().cloudTextRecognizer
        }

        bind<FirebaseMLService>() with singleton { RemoteFirebaseOcr(instance(), instance(TAG_REMOTE_SERVICE)) }
    }
}
