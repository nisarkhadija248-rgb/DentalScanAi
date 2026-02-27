package com.dentalscan.adapter

import android.graphics.BitmapFactory
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
        if (thumbnailPath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(thumbnailPath)
            if (bitmap != null) {
                holder.scanThumbnail.setImageBitmap(bitmap)
            } else {
                holder.scanThumbnail.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } else {
            holder.scanThumbnail.setImageResource(android.R.drawable.ic_menu_camera)
        }

        holder.conditionText.text = scan.condition
        holder.dateText.text = dateFormat.format(scan.timestamp)
        holder.severityText.text =
            "Confidence: ${(scan.confidence * 100).toInt()}% (${scan.severity})"

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