package com.dentalscan.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.dentalscan.R
import com.dentalscan.data.ScanEntity
import org.json.JSONArray

/**
 * Dental risk level from the profile's last 5 saved scans.
 *
 *   3 or more High-severity scans                     -> Critical
 *   2 or more High, or 3 or more Medium               -> High
 *   1 High, or 2 Medium                               -> Medium
 *   anything else                                     -> Low
 *
 * Needs at least 3 saved scans; with fewer there is no result ("Not enough data").
 * A scan's severity is the severity of its worst condition (healthy scans count as neither).
 * This is a screening aid built on the app's own confidence bands, not a medical diagnosis.
 */
object RiskAssessment {

    const val MIN_SCANS = 3
    const val WINDOW = 5

    enum class Level(
        @ColorRes val colorRes: Int,
        /** Text colour that stays readable on [colorRes] (dark text on yellow and orange). */
        @ColorRes val onColorRes: Int,
        @StringRes val labelRes: Int,
        @StringRes val reasonRes: Int,
        @StringRes val recommendationRes: Int
    ) {
        LOW(R.color.healthy_green, R.color.white, R.string.risk_low, R.string.risk_reason_low, R.string.risk_rec_low),
        MEDIUM(R.color.gingivitis_yellow, R.color.color_on_yellow, R.string.risk_medium, R.string.risk_reason_medium, R.string.risk_rec_medium),
        HIGH(R.color.calculus_orange, R.color.color_on_yellow, R.string.risk_high, R.string.risk_reason_high, R.string.risk_rec_high),
        CRITICAL(R.color.caries_red, R.color.white, R.string.risk_critical, R.string.risk_reason_critical, R.string.risk_rec_critical)
    }

    data class Result(
        val level: Level,
        /** How many scans were looked at (3 to 5). */
        val scanCount: Int,
        val highCount: Int,
        val mediumCount: Int,
        /** Distinct conditions found in those scans (Caries / Calculus / Gingivitis). */
        val conditions: List<String>
    )

    /** Null when the profile has fewer than [MIN_SCANS] saved scans. */
    fun assess(allScans: List<ScanEntity>): Result? {
        if (allScans.size < MIN_SCANS) return null

        val last = allScans.sortedByDescending { it.timestamp }.take(WINDOW)
        val high = last.count { it.severity == "High" }
        val medium = last.count { it.severity == "Medium" }

        val level = when {
            high >= 3 -> Level.CRITICAL
            high >= 2 || medium >= 3 -> Level.HIGH
            high >= 1 || medium >= 2 -> Level.MEDIUM
            else -> Level.LOW
        }

        return Result(level, last.size, high, medium, conditionsIn(last))
    }

    /** Distinct known conditions across the scans, in a fixed order. */
    private fun conditionsIn(scans: List<ScanEntity>): List<String> {
        val found = LinkedHashSet<String>()
        scans.forEach { scan ->
            val fromBoxes = try {
                val arr = JSONArray(scan.boundingBoxes)
                (0 until arr.length()).mapNotNull { ConditionStyle.of(arr.getJSONObject(it).optString("class"))?.shortName }
            } catch (e: Exception) {
                emptyList()
            }
            if (fromBoxes.isNotEmpty()) found += fromBoxes
            else ConditionStyle.of(scan.condition)?.let { found += it.shortName }
        }
        return listOf("Caries", "Calculus", "Gingivitis").filter { it in found }
    }

    /** The 2 to 3 explanation lines shown under the badge. */
    fun explanation(context: Context, r: Result): String {
        val isUrdu = LanguageManager.isUrdu(context)
        val lines = mutableListOf<String>()
        lines += context.getString(R.string.risk_explain_counts, r.scanCount, r.highCount, r.mediumCount)
        lines += if (r.conditions.isEmpty()) {
            context.getString(R.string.risk_explain_no_conditions)
        } else {
            context.getString(
                R.string.risk_explain_conditions,
                r.conditions.size,
                r.conditions.joinToString(if (isUrdu) "، " else ", ") { ConditionStyle.displayName(it, isUrdu) }
            )
        }
        lines += context.getString(r.level.reasonRes)
        return lines.joinToString("\n")
    }
}
