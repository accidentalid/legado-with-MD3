package io.legado.app.feature.reader.core.transition.waterripple

import io.legado.app.feature.reader.core.transition.ReaderPageTurnSpeed

/**
 * 水波纹翻页（iReader SmartOS 固件效果）的命令编码。
 *
 * 效果码 = 方向码 | 速度位，两者位不重叠：速度位 slow=128 / standard=64 / fast=0；
 * 方向码 1-4 表达"波纹从哪条边扫过来"，随屏幕旋转（`android.view.Surface.rotation` 0-3）
 * 查表。方向表来自 ireader.koplugin 在 Air3 Pro 上的实测修正（VeneraX 的表在横屏上不同），
 * 个别机型观感不对时只需对调 [DIRECTIONS_FORWARD] / [DIRECTIONS_BACKWARD] 的横屏项。
 */
object WaterRipplePolicy {

    /** 索引 = Surface.rotation（0=竖屏, 1=左横, 2=反向竖屏, 3=右横）。 */
    private val DIRECTIONS_FORWARD = intArrayOf(1, 3, 2, 4)
    private val DIRECTIONS_BACKWARD = intArrayOf(2, 4, 1, 3)

    /** setForceNextPostMode 的参数：把下一帧按"翻页模式"提交（ireader.koplugin 实测值）。 */
    const val FORCE_NEXT_POST_MODE = 0x01000063

    /** 复用现有翻页速度挡位 → 固件速度位：挡位越快波纹越快。 */
    fun speedFlag(speed: ReaderPageTurnSpeed): Int = when (speed) {
        ReaderPageTurnSpeed.FASTEST, ReaderPageTurnSpeed.FAST -> 0
        ReaderPageTurnSpeed.MODERATE -> 64
        ReaderPageTurnSpeed.ELEGANT -> 128
    }

    fun effectCode(forward: Boolean, rotation: Int, speed: ReaderPageTurnSpeed): Int {
        val directions = if (forward) DIRECTIONS_FORWARD else DIRECTIONS_BACKWARD
        val direction = directions[rotation.coerceIn(0, directions.lastIndex)]
        return direction or speedFlag(speed)
    }

    /** EPDCDevice.nativePostCommand 的完整命令串。 */
    fun postCommand(forward: Boolean, rotation: Int, speed: ReaderPageTurnSpeed): String =
        "next-effect-type ${effectCode(forward, rotation, speed)}"
}
