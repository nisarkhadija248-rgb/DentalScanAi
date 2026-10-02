package com.dentalscan.utils

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.dentalscan.R

/**
 * Maps the raw model label ("Caries", "gingivitis", "Calculus") to how it is shown:
 * display name, color, tint background. Used by the Analysis and History screens.
 * The raw label itself is never changed (it is what gets stored and matched elsewhere).
 */
object ConditionStyle {

    data class Style(
        val displayName: String,
        val displayNameUr: String,
        /** Short name used on the bounding-box label. */
        val shortName: String,
        @ColorRes val colorRes: Int,
        @ColorRes val tintRes: Int,
        /** Fixed ARGB used for canvas drawing, where theme colors are not available. */
        @ColorInt val paintColor: Int,
        /** Text color for text drawn on a solid [paintColor] fill (dark on yellow). */
        @ColorInt val paintTextColor: Int
    )

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val ON_YELLOW = 0xFF3E2F00.toInt()

    private val caries = Style(
        displayName = "Dental Caries",
        displayNameUr = "کیریز (سڑن)",
        shortName = "Caries",
        colorRes = R.color.caries_red,
        tintRes = R.color.caries_bg,
        paintColor = 0xFFD32F2F.toInt(),
        paintTextColor = WHITE
    )

    private val calculus = Style(
        displayName = "Dental Calculus",
        displayNameUr = "کیلکولس (پتھر)",
        shortName = "Calculus",
        colorRes = R.color.calculus_orange,
        tintRes = R.color.calculus_bg,
        paintColor = 0xFFFB8C00.toInt(),
        paintTextColor = ON_YELLOW
    )

    private val gingivitis = Style(
        displayName = "Gingivitis",
        displayNameUr = "مسوڑوں کی سوزش",
        shortName = "Gingivitis",
        colorRes = R.color.gingivitis_yellow,
        tintRes = R.color.gingivitis_bg,
        paintColor = 0xFFFBC02D.toInt(),
        paintTextColor = ON_YELLOW
    )

    /** Returns the style for a raw model label, or null if the label is not one of the three. */
    fun of(raw: String?): Style? = when (raw?.trim()?.lowercase()) {
        "caries" -> caries
        "calculus" -> calculus
        "gingivitis" -> gingivitis
        else -> null
    }

    fun displayName(raw: String?, isUrdu: Boolean): String {
        val style = of(raw)
        return when {
            style != null -> if (isUrdu) style.displayNameUr else style.displayName
            raw.isNullOrBlank() -> ""
            else -> raw.replaceFirstChar { it.uppercase() }
        }
    }

    fun shortName(raw: String?): String =
        of(raw)?.shortName ?: (raw ?: "").replaceFirstChar { it.uppercase() }

    /** Theme-aware condition color (falls back to the primary color for unknown labels). */
    @ColorInt
    fun color(context: Context, raw: String?): Int =
        ContextCompat.getColor(context, of(raw)?.colorRes ?: R.color.primary)

    /** Theme-aware light tint used for chip / card backgrounds. */
    @ColorInt
    fun tint(context: Context, raw: String?): Int =
        ContextCompat.getColor(context, of(raw)?.tintRes ?: R.color.surface_variant)

    /** Fixed ARGB for canvas drawing. */
    @ColorInt
    fun paintColor(raw: String?): Int = of(raw)?.paintColor ?: 0xFF388E3C.toInt()

    /** Text color to use on top of [paintColor]. */
    @ColorInt
    fun paintTextColor(raw: String?): Int = of(raw)?.paintTextColor ?: WHITE

    fun severityLabel(severity: String, isUrdu: Boolean): String =
        if (isUrdu) {
            when (severity) {
                "High" -> "زیادہ"
                "Medium" -> "درمیانی"
                "Low" -> "کم"
                else -> severity
            }
        } else {
            severity
        }
}
