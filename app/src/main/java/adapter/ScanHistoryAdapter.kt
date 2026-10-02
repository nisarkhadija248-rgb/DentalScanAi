package com.dentalscan.adapter

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import com.dentalscan.utils.ConditionStyle
import com.dentalscan.utils.LanguageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.R
import com.dentalscan.data.ScanEntity
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
        val bitmap = if (thumbnailPath.isNotEmpty()) BitmapFactory.decodeFile(thumbnailPath) else null
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
        holder.conditionText.text = ConditionStyle.displayName(scan.condition, isUrdu)
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

        holder.severityText.text =
            "${(scan.confidence * 100).toInt()}% · ${ConditionStyle.severityLabel(scan.severity, isUrdu)}"

        holder.itemView.setOnClickListener {
            onItemClick(scan)
        }
    }

    override fun getItemCount() = scans.size

    fun submitList(newScans: List<ScanEntity>) {
        scans = newScans
        notifyDataSetChanged()
    }
}