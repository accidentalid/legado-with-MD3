package io.legado.app.feature.reader.core.transition.waterripple

import io.legado.app.feature.reader.core.transition.ReaderPageTurnSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 水波纹翻页命令编码：效果码 = 方向码 | 速度位（位不重叠，OR 等价于相加），
 * 方向码按 Surface.rotation 0-3 查表，速度位复用现有翻页速度挡位。
 */
class WaterRipplePolicyTest {

    @Test
    fun `速度挡位映射到固件速度位`() {
        assertEquals(0, WaterRipplePolicy.speedFlag(ReaderPageTurnSpeed.FASTEST))
        assertEquals(0, WaterRipplePolicy.speedFlag(ReaderPageTurnSpeed.FAST))
        assertEquals(64, WaterRipplePolicy.speedFlag(ReaderPageTurnSpeed.MODERATE))
        assertEquals(128, WaterRipplePolicy.speedFlag(ReaderPageTurnSpeed.ELEGANT))
    }

    @Test
    fun `方向码按旋转查表并与速度位合并`() {
        // ireader.koplugin 实测表：forward = [1,3,2,4]，backward = [2,4,1,3]。
        for (rotation in 0..3) {
            assertEquals(
                intArrayOf(1, 3, 2, 4)[rotation] or 64,
                WaterRipplePolicy.effectCode(forward = true, rotation = rotation, speed = ReaderPageTurnSpeed.MODERATE),
            )
            assertEquals(
                intArrayOf(2, 4, 1, 3)[rotation] or 64,
                WaterRipplePolicy.effectCode(forward = false, rotation = rotation, speed = ReaderPageTurnSpeed.MODERATE),
            )
        }
    }

    @Test
    fun `竖屏标准速度的前翻效果码是 65`() {
        // Air3 Pro 实测值：1 | 64 = 65。
        assertEquals(
            65,
            WaterRipplePolicy.effectCode(forward = true, rotation = 0, speed = ReaderPageTurnSpeed.MODERATE),
        )
    }

    @Test
    fun `命令串格式为 next-effect-type 加效果码`() {
        assertEquals(
            "next-effect-type 65",
            WaterRipplePolicy.postCommand(forward = true, rotation = 0, speed = ReaderPageTurnSpeed.MODERATE),
        )
        assertEquals(
            "next-effect-type ${2 or 128}",
            WaterRipplePolicy.postCommand(forward = false, rotation = 0, speed = ReaderPageTurnSpeed.ELEGANT),
        )
    }

    @Test
    fun `越界旋转值钳回表内`() {
        assertEquals(
            WaterRipplePolicy.effectCode(forward = true, rotation = 0, speed = ReaderPageTurnSpeed.MODERATE),
            WaterRipplePolicy.effectCode(forward = true, rotation = -1, speed = ReaderPageTurnSpeed.MODERATE),
        )
        assertEquals(
            WaterRipplePolicy.effectCode(forward = true, rotation = 3, speed = ReaderPageTurnSpeed.MODERATE),
            WaterRipplePolicy.effectCode(forward = true, rotation = 9, speed = ReaderPageTurnSpeed.MODERATE),
        )
    }
}
