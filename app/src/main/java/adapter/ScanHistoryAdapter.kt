package com.dentalscan.adapter

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.PhotoLoader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.R
import com.dentalscan.data.ScanEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

class ScanHistoryAdapter(
    private val onItemClick: (ScanEntity) -> Unit
) : RecyclerView.Adapter<ScanHistoryAdapter.ViewHolder>() {

    private var scans: List<ScanEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val scanThumbnail: ImageView = view.findViewById(R.id.scanThumbnail)
        val conditionText: TextView = view.findViewById(R.id.conditionText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val severityText: TextView = view.findViewById(R.id.severityText)
        val conditionStripe: View = view.findViewById(R.id.conditionStripe)
        val photoCountText: TextView = view.findViewById(R.id.photoCountText)
        val defaultConditionColors: ColorStateList = conditionText.textColors
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scan = scans[position]

        // Load thumbnail
        val thumbnailPath = scan.getThumbnailPath()
        // All rows are laid out together now, so decode a small version of each photo (56dp thumbnail)
        val bitmap = if (thumbnailPath.isNotEmpty()) PhotoLoader.decodeSampled(thumbnailPath, 120) else null
        if (bitmap != null) {
            holder.scanThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.scanThumbnail.setPadding(0, 0, 0, 0)
            holder.scanThumbnail.imageTintList = null
            holder.scanThumbnail.setImageBitmap(bitmap)
        } else {
            // Small tinted camera icon instead of a stretched placeholder
            val pad = (14 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.scanThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.scanThumbnail.setPadding(pad, pad, pad, pad)
            holder.scanThumbnail.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
            )
            holder.scanThumbnail.setImageResource(R.drawable.ic_camera)
        }

        val context = holder.itemView.context
        val isUrdu = LanguageManager.isUrdu(context)
        // Every condition found in the scan ("Caries, Calculus"); a single one keeps its full name
        val found = conditionsFound(scan)
        holder.conditionText.text = if (found.size > 1) {
            found.joinToString(if (isUrdu) "، " else ", ") {
                if (isUrdu) ConditionStyle.displayName(it, true) else ConditionStyle.shortName(it)
            }
        } else {
            ConditionStyle.displayName(scan.condition, isUrdu)
        }
        holder.conditionStripe.setBackgroundColor(ConditionStyle.color(context, scan.condition))
        holder.dateText.text = dateFormat.format(scan.timestamp)

        // "N photos" badge when the scan has several captured angles
        val photoCount = scan.getAllImagePaths().count { it.isNotEmpty() }
        if (photoCount > 1) {
            holder.photoCountText.text = if (isUrdu) "$photoCount تصاویر" else "$photoCount photos"
            holder.photoCountText.visibility = View.VISIBLE
        } else {
            holder.photoCountText.visibility = View.GONE
        }

        val healthy = ConditionStyle.isHealthy(scan.condition)
        holder.severityText.text = if (healthy) {
            context.getString(R.string.healthy_row_note)
        } else {
            "${(scan.confidence * 100).toInt()}% · ${ConditionStyle.severityLabel(scan.severity, isUrdu)}"
        }

        // Healthy scans: green badge with a check instead of a condition-coloured title.
        // (Rows are recycled, so every other row is explicitly reset.)
        val density = context.resources.displayMetrics.density
        if (healthy) {
            val green = ContextCompat.getColor(context, R.color.healthy_green)
            holder.conditionText.setTextColor(green)
            holder.conditionText.setBackgroundResource(R.drawable.bg_badge)
            holder.conditionText.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.healthy_bg))
            holder.conditionText.setPadding(
                (8 * density).toInt(), (3 * density).toInt(), (10 * density).toInt(), (3 * density).toInt()
            )
            val check = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)?.mutate()
            check?.setBounds(0, 0, (16 * density).toInt(), (16 * density).toInt())
            check?.setTint(green)
            holder.conditionText.compoundDrawablePadding = (6 * density).toInt()
            holder.conditionText.setCompoundDrawablesRelative(check, null, null, null)
        } else {
            holder.conditionText.setTextColor(holder.defaultConditionColors)
            holder.conditionText.background = null
            holder.conditionText.backgroundTintList = null
            holder.conditionText.setPadding(0, 0, 0, 0)
            holder.conditionText.setCompoundDrawablesRelative(null, null, null, null)
        }

        holder.itemView.setOnClickListener {
            onItemClick(scan)
        }
    }

    /**
     * Distinct known conditions saved with the scan, highest confidence first. Reads the saved
     * detections; anything unreadable gives an empty list (the row then shows the main condition).
     */
    private fun conditionsFound(scan: ScanEntity): List<String> {
        if (scan.boundingBoxes.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(scan.boundingBoxes)
            (0 until arr.length())
                .map { arr.getJSONObject(it) }
                .mapNotNull { o ->
                    ConditionStyle.of(o.optString("class"))?.let { it.shortName to o.optDouble("confidence", 0.0) }
                }
                .groupBy({ it.first }, { it.second })
                .map { (name, confs) -> name to confs.max() }
                .sortedByDescending { it.second }
                .map { it.first }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getItemCount() = scans.size

    fun submitList(newScans: List<ScanEntity>) {
        scans = newScans
        notifyDataSetChanged()
    }
}