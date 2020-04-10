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

package taiwan.no.one.taggerprice.presentation.lifecycle

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import taiwan.no.one.taggerprice.di.FeatModuleHelper
import taiwan.no.one.taggerprice.provider.NaviGraphRouteProvider

class SplitModuleAddLifecycle(
    private val context: Context,
    private val modules: List<String>
) : DefaultLifecycleObserver {
    private val manager by lazy { SplitInstallManagerFactory.create(context.applicationContext) }
    private val request by lazy {
        SplitInstallRequest.newBuilder().apply { modules.forEach { addModule(it) } }.build()
    }
    private val listener by lazy {
        val captureRoute = "${FeatModuleHelper.featurePackagePrefix}.capture.FeatureARoute"
        SplitInstallStateUpdatedListener {
            when (it.status()) {
                SplitInstallSessionStatus.INSTALLED -> {
                    val route = Class.forName(captureRoute).kotlin.objectInstance as? NaviGraphRouteProvider
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        manager.registerListener(listener)
        manager.startInstall(request)
    }

    override fun onPause(owner: LifecycleOwner) {
        manager.unregisterListener(listener)
    }
}
