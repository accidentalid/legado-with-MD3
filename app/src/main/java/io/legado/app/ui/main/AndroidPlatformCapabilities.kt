package io.legado.app.ui.main

import android.os.Build
import android.view.RoundedCorner
import androidx.appcompat.app.AppCompatActivity

/**
 * 平台能力采样（对照 shutiao 版 AndroidPlatformCapabilities，按需增量补充，不预先铺接口）。
 */
class AndroidPlatformCapabilities(private val activity: AppCompatActivity) {

    /**
     * 屏幕圆角半径 px：四角中非零者的最小值（忽略报告为 0 的角），
     * 取不到或全 0 时回退 [DEFAULT_DISPLAY_CORNER_RADIUS_PX]
     * （对照参考实现 AppTransitionInjector.initDisplayRoundCorner 语义）。
     * 半径可能返回 null（设备未报告圆角、部分多窗口模式），null ≠ 无圆角。
     * 不缓存：折叠屏切换屏幕后半径会变；取值来自 DisplayInfo，很廉价。
     */
    val displayCornerRadiusPx: Float
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return DEFAULT_DISPLAY_CORNER_RADIUS_PX
            }
            val display = activity.display ?: return DEFAULT_DISPLAY_CORNER_RADIUS_PX
            val radii = listOf(
                RoundedCorner.POSITION_TOP_LEFT,
                RoundedCorner.POSITION_TOP_RIGHT,
                RoundedCorner.POSITION_BOTTOM_RIGHT,
                RoundedCorner.POSITION_BOTTOM_LEFT,
            ).map { display.getRoundedCorner(it)?.radius?.toFloat() ?: 0f }
            return selectCornerRadiusPx(radii)
        }

    /**
     * 当前屏幕旋转（`android.view.Surface.rotation` 0-3），水波纹翻页的方向码索引。
     * 取不到回落 0（自然竖屏）；不缓存，跟随旋转随时可变。
     */
    val displayRotation: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.rotation ?: 0
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.rotation
        }

    companion object {
        const val DEFAULT_DISPLAY_CORNER_RADIUS_PX = 60f

        /** 纯选择逻辑：非零角取最小，全零回退（采样与单测共用）。 */
        fun selectCornerRadiusPx(
            radii: List<Float>,
            fallback: Float = DEFAULT_DISPLAY_CORNER_RADIUS_PX,
        ): Float = radii.filter { it > 0f }.minOrNull() ?: fallback
    }
}
