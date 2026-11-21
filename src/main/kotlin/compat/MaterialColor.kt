package com.google.android.material.color

import io.material.hct.Hct
import io.material.utils.MathUtils

object DynamicColors {
    /** Wallpaper-based colours are an Android 12+ system feature. */
    fun isDynamicColorAvailable(): Boolean = false
}

object MaterialColors {
    /** Same algorithm as Material's Blend.harmonize: shift the hue towards the source colour. */
    fun harmonize(colorToHarmonize: Int, colorToHarmonizeWith: Int): Int {
        val fromHct = Hct.fromInt(colorToHarmonize)
        val toHct = Hct.fromInt(colorToHarmonizeWith)
        val differenceDegrees = MathUtils.differenceDegrees(fromHct.hue, toHct.hue)
        val rotationDegrees = minOf(differenceDegrees * 0.5, 15.0)
        val outputHue =
            MathUtils.sanitizeDegreesDouble(
                fromHct.hue + rotationDegrees * MathUtils.rotationDirection(fromHct.hue, toHct.hue)
            )
        return Hct.from(outputHue, fromHct.chroma, fromHct.tone).toInt()
    }
}
