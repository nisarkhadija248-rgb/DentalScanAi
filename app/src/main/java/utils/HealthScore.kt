package com.dentalscan.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.dentalscan.R
import com.dentalscan.data.AppDatabase
import com.dentalscan.data.ScanEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Dental Health Score, 0..100, per profile.
 *
 * Rules (each saved scan, oldest first, starting from 100):
 *  - healthy scan:                    +5
 *  - each condition found:            Low -5, Medium -10, High -20   (several conditions: the penalties add up)
 *  - the score is kept between 0 and 100 after every scan.
 * A profile has no score until its first scan.
 *
 * The score is always computed from the profile's saved scans (a pure function of the scan list), so it
 * can never drift from the history: it repairs itself after "Clear history" or for scans saved before this
 * feature existed. profiles.healthScore is the saved copy, refreshed by [sync].
 */
object HealthScore {

    const val START = 100
    private const val HEALTHY_BONUS = 5
    private const val PENALTY_LOW = 5
    private const val PENALTY_MEDIUM = 10
    private const val PENALTY_HIGH = 20

    enum class Level(@ColorRes val colorRes: Int, @StringRes val labelRes: Int) {
        NEEDS_ATTENTION(R.color.caries_red, R.string.health_needs_attention),   // 0 to 40
        FAIR(R.color.calculus_orange, R.string.health_fair),                    // 41 to 70
        GOOD(R.color.healthy_green, R.string.health_good)                       // 71 to 100
    }

    fun levelOf(score: Int): Level = when {
        score <= 40 -> Level.NEEDS_ATTENTION
        score <= 70 -> Level.FAIR
        else -> Level.GOOD
    }

    private fun penaltyFor(severity: String): Int = when (severity) {
        "High" -> PENALTY_HIGH
        "Medium" -> PENALTY_MEDIUM
        else -> PENALTY_LOW
    }

    /** How much one scan changes the score (positive for healthy, negative otherwise). */
    fun delta(scan: ScanEntity): Int {
        if (ConditionStyle.isHealthy(scan.condition)) return HEALTHY_BONUS

        // Every condition found in the scan (highest confidence per condition) adds its own penalty
        val perCondition: Map<String, Float> = try {
            val arr = JSONArray(scan.boundingBoxes)
            val best = HashMap<String, Float>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = ConditionStyle.of(o.optString("class"))?.shortName ?: continue
                val conf = o.optDouble("confidence", 0.0).toFloat()
                if (conf > (best[name] ?: -1f)) best[name] = conf
            }
            best
        } catch (e: Exception) {
            emptyMap()
        }

        if (perCondition.isEmpty()) {
            // Older scan without saved detections: use its one main condition
            return -penaltyFor(scan.severity)
        }
        return -perCondition.values.sumOf { penaltyFor(ImageUtils.getSeverityText(it)) }
    }

    /** Score after all [scans] (any order), or null when there are none. */
    fun compute(scans: List<ScanEntity>): Int? {
        if (scans.isEmpty()) return null
        var score = START
        scans.sortedBy { it.timestamp }.forEach { score = (score + delta(it)).coerceIn(0, 100) }
        return score
    }

    /** Change over the last [days] days: current score minus the score before that window. */
    fun trend(scans: List<ScanEntity>, days: Int = 30, now: Date = Date()): Int {
        val current = compute(scans) ?: return 0
        val cutoff = now.time - TimeUnit.DAYS.toMillis(days.toLong())
        val before = compute(scans.filter { it.timestamp.time < cutoff }) ?: START
        return current - before
    }

    // ---------- Database helpers ----------

    /** Saved scans of the active profile (all scans when no profile is active). */
    suspend fun scansFor(context: Context): List<ScanEntity> {
        val dao = AppDatabase.getDatabase(context).scanDao()
        val profileId = ProfileManager.getActiveProfileId(context)
        return if (profileId == -1L) dao.getAllScans().first()
        else dao.getScansForUser(profileId.toString()).first()
    }

    /** Write the computed score to the active profile's row (100 when it has no scans). */
    suspend fun sync(context: Context, score: Int?) {
        val profileId = ProfileManager.getActiveProfileId(context)
        if (profileId <= 0L) return
        val dao = AppDatabase.getDatabase(context).profileDao()
        val target = score ?: START
        if (dao.getProfileById(profileId)?.healthScore != target) {
            dao.updateHealthScore(profileId, target)
        }
    }
}
