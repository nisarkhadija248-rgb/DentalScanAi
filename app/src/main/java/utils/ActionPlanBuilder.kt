package com.dentalscan.utils

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dentalscan.R
import org.json.JSONArray

/**
 * Builds the interactive Action Plan (header, headline, checklist steps and quick tips)
 * from the conditions found in a scan.
 *
 * - One condition: that condition's steps. Several conditions: one combined plan that leads with
 *   the most severe urgency, lists each condition's steps (most severe first), then general care.
 * - All text comes from strings.xml, so it follows the in-app language (pass an Activity context,
 *   BaseActivity already applies the chosen locale to it).
 * - The result is a pure function of the findings. The checklist stores step INDICES, so the same
 *   findings must always produce the same steps in the same order. Keep that in mind before
 *   reordering or inserting steps: old saved ticks would then point at different steps.
 */
object ActionPlanBuilder {

    enum class Urgency { HIGH, MEDIUM, LOW }

    /** One detected condition: the raw model label and its confidence (0..1). */
    data class Finding(val condition: String, val confidence: Float)

    data class Tip(
        @DrawableRes val iconRes: Int,
        val title: String,
        val summary: String,
        val fullText: String
    )

    data class Plan(
        val urgency: Urgency,
        /** "Urgent Action Required" / "Action Recommended" / "Maintenance Required" */
        val header: String,
        /** e.g. "Urgent: Visit a dentist within 2 weeks" */
        val headline: String,
        /** Only for combined plans, e.g. "2 conditions found". Null for a single condition. */
        val foundLine: String?,
        val steps: List<String>,
        val tips: List<Tip>,
        /** Short names of the conditions in the plan, most severe first (Caries / Calculus / Gingivitis). */
        val conditions: List<String>
    ) {
        val isCombined: Boolean get() = conditions.size > 1
    }

    // ---------- Content tables (resource ids only; text is resolved at build time) ----------

    private class TipRes(
        @DrawableRes val icon: Int,
        @StringRes val title: Int,
        @StringRes val summary: Int,
        @StringRes val full: Int
    )

    private class ConditionContent(
        val steps: List<Int>, // string resource ids
        val tips: List<TipRes>
    )

    private val content: Map<String, ConditionContent> = mapOf(
        "Caries" to ConditionContent(
            steps = listOf(R.string.plan_step_caries_1, R.string.plan_step_caries_2),
            tips = listOf(
                TipRes(R.drawable.ic_restaurant, R.string.tip_caries_1_title, R.string.tip_caries_1_line, R.string.tip_caries_1_full),
                TipRes(R.drawable.ic_brush, R.string.tip_caries_2_title, R.string.tip_caries_2_line, R.string.tip_caries_2_full),
                TipRes(R.drawable.ic_event, R.string.tip_caries_3_title, R.string.tip_caries_3_line, R.string.tip_caries_3_full)
            )
        ),
        "Calculus" to ConditionContent(
            steps = listOf(R.string.plan_step_calculus_1, R.string.plan_step_calculus_2),
            tips = listOf(
                TipRes(R.drawable.ic_medical_services, R.string.tip_calculus_1_title, R.string.tip_calculus_1_line, R.string.tip_calculus_1_full),
                TipRes(R.drawable.ic_brush, R.string.tip_calculus_2_title, R.string.tip_calculus_2_line, R.string.tip_calculus_2_full),
                TipRes(R.drawable.ic_event, R.string.tip_calculus_3_title, R.string.tip_calculus_3_line, R.string.tip_calculus_3_full)
            )
        ),
        "Gingivitis" to ConditionContent(
            steps = listOf(R.string.plan_step_gingivitis_1, R.string.plan_step_gingivitis_2),
            tips = listOf(
                TipRes(R.drawable.ic_brush, R.string.tip_gingivitis_1_title, R.string.tip_gingivitis_1_line, R.string.tip_gingivitis_1_full),
                TipRes(R.drawable.ic_check_circle, R.string.tip_gingivitis_2_title, R.string.tip_gingivitis_2_line, R.string.tip_gingivitis_2_full),
                TipRes(R.drawable.ic_warning, R.string.tip_gingivitis_3_title, R.string.tip_gingivitis_3_line, R.string.tip_gingivitis_3_full)
            )
        )
    )

    private val generalSteps = listOf(R.string.plan_step_general_1, R.string.plan_step_general_2)

    private val generalTip = TipRes(
        R.drawable.ic_event,
        R.string.tip_general_1_title, R.string.tip_general_1_line, R.string.tip_general_1_full
    )

    private const val MAX_TIPS = 4

    // ---------- Building ----------

    /** Same bands as the saved severity, so a plan never disagrees with the severity chip. */
    private fun urgencyOf(confidence: Float): Urgency = when (ImageUtils.getSeverityText(confidence)) {
        "High" -> Urgency.HIGH
        "Medium" -> Urgency.MEDIUM
        else -> Urgency.LOW
    }

    /**
     * @param findings every detection (a condition may appear several times; the highest
     *                 confidence per condition is used). Unknown labels are ignored.
     */
    fun build(context: Context, findings: List<Finding>): Plan {
        // One entry per known condition, most severe first. Fully deterministic ordering:
        // urgency, then confidence (high to low), then name.
        val perCondition: List<Pair<String, Float>> = findings
            .mapNotNull { f -> ConditionStyle.of(f.condition)?.let { it.shortName to f.confidence } }
            .groupBy({ it.first }, { it.second })
            .map { (name, confidences) -> name to confidences.max() }
            .sortedWith(
                compareBy<Pair<String, Float>>(
                    { urgencyOf(it.second).ordinal },
                    { -it.second },
                    { it.first }
                )
            )

        val urgency = if (perCondition.isEmpty()) {
            Urgency.LOW
        } else {
            urgencyOf(perCondition.maxOf { it.second })
        }

        val headerRes = when (urgency) {
            Urgency.HIGH -> R.string.plan_header_high
            Urgency.MEDIUM -> R.string.plan_header_medium
            Urgency.LOW -> R.string.plan_header_low
        }
        val headlineRes = when (urgency) {
            Urgency.HIGH -> R.string.plan_headline_high
            Urgency.MEDIUM -> R.string.plan_headline_medium
            Urgency.LOW -> R.string.plan_headline_low
        }
        val dentistStepRes = when (urgency) {
            Urgency.HIGH -> R.string.plan_step_dentist_high
            Urgency.MEDIUM -> R.string.plan_step_dentist_medium
            Urgency.LOW -> R.string.plan_step_dentist_low
        }

        // Steps: dentist first (set by the most severe condition), then each condition's
        // own steps in severity order, then general prevention last.
        val stepRes = mutableListOf(dentistStepRes)
        perCondition.forEach { (name, _) -> content[name]?.let { stepRes += it.steps } }
        stepRes += generalSteps

        // Tips: round-robin across conditions (most severe first), then one general tip, max 4.
        val tipLists = perCondition.mapNotNull { content[it.first]?.tips }
        val tipRes = mutableListOf<TipRes>()
        var round = 0
        while (tipRes.size < MAX_TIPS && tipLists.any { round < it.size }) {
            for (list in tipLists) {
                if (round < list.size && tipRes.size < MAX_TIPS) tipRes += list[round]
            }
            round++
        }
        if (tipRes.size < MAX_TIPS) tipRes += generalTip

        val names = perCondition.map { it.first }
        return Plan(
            urgency = urgency,
            header = context.getString(headerRes),
            headline = context.getString(headlineRes),
            foundLine = if (names.size > 1) context.getString(R.string.plan_multiple_found, names.size) else null,
            steps = stepRes.map { context.getString(it) },
            tips = tipRes.map {
                Tip(
                    iconRes = it.icon,
                    title = context.getString(it.title),
                    summary = context.getString(it.summary),
                    fullText = context.getString(it.full)
                )
            },
            conditions = names
        )
    }

    // ---------- Checklist state <-> JSON (stored in ScanEntity.checkedSteps) ----------

    /** "[0,2]" -> {0, 2}. Null, blank or invalid JSON -> empty set. */
    fun parseChecked(json: String?): Set<Int> {
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getInt(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** {2, 0} -> "[0,2]" (sorted, so the stored value is stable). Empty set -> null. */
    fun encodeChecked(checked: Set<Int>): String? {
        if (checked.isEmpty()) return null
        return JSONArray(checked.sorted()).toString()
    }
}
