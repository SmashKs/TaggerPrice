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

package config

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private const val FEATURE_PREFIX = ":feature_"

@Suppress("unused")
object CommonModuleDependency {
    const val APP = ":app"
    const val LIB_PURE_EXT = ":library_ext"
    const val LIB_KTX = ":library_ktx"
    const val LIB_WIDGET = ":library_widget"
    const val LIB_DEVICE = ":library_device"
    const val LIB_CORE = ":library_core"
    const val FEAT_DUMMY = ":feature_featDummy"

    fun getAllModules() = CommonModuleDependency::class.memberProperties
        .asSequence()
        .filter(KProperty1<CommonModuleDependency, *>::isConst)
        .map { it.getter.call().toString() }
        .toSet()

    fun getDynamicFeatureModules() = getAllModules()
        .asSequence()
        .filter { it.startsWith(FEATURE_PREFIX) }
        .toSet()

    fun getFeatureModuleName() = getDynamicFeatureModules()
        .asSequence()
        .map { it.replace("feature_", "") }
        .toMutableSet()
}
