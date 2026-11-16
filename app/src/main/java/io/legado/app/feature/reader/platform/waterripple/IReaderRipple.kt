package io.legado.app.feature.reader.platform.waterripple

import android.os.Build
import android.util.Log
import io.legado.app.feature.reader.core.transition.ReaderPageTurnSpeed
import io.legado.app.feature.reader.core.transition.waterripple.WaterRipplePolicy
import java.lang.reflect.Method

/**
 * iReader SmartOS 水波纹翻页桥接（对照 VeneraX EinkPanel / ireader.koplugin epdc.lua）。
 *
 * 固件隐藏类 `android.eink.EPDCDevice` 的静态 `nativePostCommand(String)` 接收文本命令
 * `"next-effect-type <方向码|速度位>"`（编码见 [WaterRipplePolicy]），墨水屏固件在
 * "下一次整帧提交"时播放系统级水波纹，因此命令必须在新页内容上屏**之前**发出。
 * 非 iReader 设备上 [supported] 为 false，所有调用都是空操作、不碰反射。
 */
object IReaderRipple {

    private const val TAG = "IReaderRipple"

    private val postCommandMethod: Method? by lazy {
        runCatching {
            Class.forName("android.eink.EPDCDevice")
                .getMethod("nativePostCommand", String::class.java)
        }.getOrNull()
    }

    private val forceNextPostModeMethod: Method? by lazy {
        runCatching {
            Class.forName("android.eink.EPDCDevice")
                .getMethod("setForceNextPostMode", Int::class.javaPrimitiveType)
        }.getOrNull()
    }

    /** 身份探测在前（短路），非 iReader 设备完全不解析反射方法。 */
    val supported: Boolean by lazy {
        val vendor = Build.MANUFACTURER.equals("iReader", ignoreCase = true) ||
            Build.BRAND.equals("iReader", ignoreCase = true)
        vendor && postCommandMethod != null
    }

    /** 翻页提交前调用；失败只记日志并静默回退（本页无波纹，翻页本身不受影响）。 */
    fun queuePageTurnRipple(forward: Boolean, rotation: Int, speed: ReaderPageTurnSpeed): Boolean {
        if (!supported) return false
        val command = WaterRipplePolicy.postCommand(forward, rotation, speed)
        return runCatching {
            postCommandMethod?.invoke(null, command)
            // best-effort：部分固件需要强制下一帧按翻页模式提交，方法缺失时忽略。
            forceNextPostModeMethod?.invoke(null, WaterRipplePolicy.FORCE_NEXT_POST_MODE)
            true
        }.onFailure { Log.w(TAG, "queue page turn ripple failed: ${it.message}") }
            .getOrDefault(false)
    }
}
