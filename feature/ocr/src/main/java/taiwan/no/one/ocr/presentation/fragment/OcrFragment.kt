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

package taiwan.no.one.ocr.presentation.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import com.devrapid.kotlinknifer.logw
import com.devrapid.kotlinknifer.toBitmap
import com.devrapid.kotlinknifer.toDrawable
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import taiwan.no.one.core.presentation.activity.BaseActivity
import taiwan.no.one.core.presentation.fragment.BaseFragment
import taiwan.no.one.ktx.context.allPermissionsGranted
import taiwan.no.one.ocr.R
import taiwan.no.one.ocr.databinding.FragmentOcrBinding
import taiwan.no.one.ocr.ml.KerasOcrDr
import taiwan.no.one.ocr.presentation.viewmodel.OcrViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

internal class OcrFragment : BaseFragment<BaseActivity<*>, FragmentOcrBinding>() {
    private val requiredPermissions =
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /**
     * TessBaseAPI初始化用到的第一个参数，是个目录。
     */
    private val DATAPATH: String =
        Environment.getExternalStorageDirectory().absolutePath.toString() + File.separator

    /**
     * 在DATAPATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private val tessdata = DATAPATH + "tessdata"

    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private val DEFAULT_LANGUAGE = "eng"

    /**
     * assets中的文件名
     */
    private val DEFAULT_LANGUAGE_NAME = "$DEFAULT_LANGUAGE.traineddata"

    /**
     * 保存到SD卡中的完整文件名
     */
    private val LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME
    private val PERMISSION_REQUEST_CODE = 0

    private val vm by viewModel<OcrViewModel>()

    override fun bindLiveData() {
        vm.ocrResult.observe(this) {
            it.onSuccess {
                logw(it)
            }.onFailure {
                logw(it.localizedMessage.toString())
            }
        }
    }

    override fun rendered(savedInstanceState: Bundle?) {
        val bitmap = R.drawable.test_ocr_1.toDrawable(requireContext()).toBitmap()
        binding.ivPic.setImageBitmap(bitmap)

        if (Build.VERSION.SDK_INT >= 23) {
            if (requireContext().allPermissionsGranted(requiredPermissions)) {
                requestPermissions(requiredPermissions, PERMISSION_REQUEST_CODE)
                return
            }
        }

//        copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
//        val tessapi = TessBaseAPI().apply {
//            val datapath = Environment.getExternalStorageDirectory().absolutePath + File.separator
//            logw(datapath)
//            init(datapath, "eng")
//            pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
//        }
//
//        tessapi.setImage(R.drawable.ocr_test.toDrawable(requireContext()).toBitmap())
//        logw(tessapi.utF8Text)
//        tessapi.end()

        // Firebase way
        val visionImage = FirebaseVisionImage.fromBitmap(bitmap)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        val task = detector.processImage(visionImage)
        task.addOnSuccessListener {
            binding.mtvText.text = it.text
            // draw on the bitmap
            val bm = bitmap.copy(bitmap.config, true)
            Canvas(bm).apply {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }
                it.textBlocks.forEach {
                    it.boundingBox?.let { drawRect(it, paint) }
                }
            }

            binding.ivPic.setImageBitmap(bm)
        }

        // TensorFlow Lite
        val model = KerasOcrDr.newInstance(requireContext())

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 31, 200, 1), DataType.FLOAT32)
        val bytes = bitmap.byteCount
        val byteBuffer = ByteBuffer.allocate(bytes)
        bitmap.copyPixelsToBuffer(byteBuffer)
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // Releases model resources if no longer used.
        model.close()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                logw("onRequestPermissionsResult: copy")
                copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME)
            }
            else -> {
            }
        }
    }

    fun copyToSD(path: String, name: String) {
        logw("copyToSD: $path")
        logw("copyToSD: $name")
        // 如果存在就删掉
        val f = File(path)
        if (f.exists()) {
            f.delete()
        }
        if (!f.exists()) {
            val p = File(f.parent)
            if (!p.exists()) {
                p.mkdirs()
            }
            try {
                f.createNewFile()
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
        var `is`: InputStream? = null
        var os: OutputStream? = null
        try {
            `is` = requireContext().applicationContext.assets.open(name)
            val file = File(path)
            os = FileOutputStream(file)
            val bytes = ByteArray(2048)
            var len = 0
            while (`is`.read(bytes).also { len = it } != -1) {
                os.write(bytes, 0, len)
            }
            os.flush()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
        finally {
            try {
                `is`?.close()
                os?.close()
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
