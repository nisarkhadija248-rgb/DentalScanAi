package com.dentalscan.utils

import android.content.Context
import com.dentalscan.R

/**
 * Names for the five capture angles. The list of photo paths saved with a scan has one entry per
 * capture step (a skipped step is ""), so the position in the list is the angle:
 * 0 = Front, 1 = Upper teeth, 2 = Lower teeth, 3 = Left side, 4 = Right side.
 *
 * Scans saved before skipped steps were stored as "" only contain the photos that were taken, so
 * the position of anything after the front view may not match its step. Those are shown as
 * "Photo N" instead of a possibly wrong angle name.
 */
object AngleNames {

    const val STEP_COUNT = 5

    /** True when the list has one slot per step, so positions can be trusted as angles. */
    fun positionsAreAngles(paths: List<String>): Boolean = paths.size == STEP_COUNT

    /**
     * @param positionsTrusted result of [positionsAreAngles] for the scan's path list.
     * The front view (index 0) is always correct: step 1 can never be skipped.
     */
    fun label(context: Context, index: Int, positionsTrusted: Boolean): String {
        if (index == 0) return context.getString(R.string.angle_front)
        if (!positionsTrusted) return context.getString(R.string.angle_photo_n, index + 1)
        return when (index) {
            1 -> context.getString(R.string.angle_upper)
            2 -> context.getString(R.string.angle_lower)
            3 -> context.getString(R.string.angle_left)
            4 -> context.getString(R.string.angle_right)
            else -> context.getString(R.string.angle_photo_n, index + 1)
        }
    }
}
