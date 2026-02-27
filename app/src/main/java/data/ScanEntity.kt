package com.dentalscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.util.Date

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val userName: String = "",
    val imagePath: String = "",  // JSON array of paths e.g. ["path1.jpg","path2.jpg"]
    val timestamp: Date = Date(),
    val condition: String = "",
    val confidence: Float = 0f,
    val severity: String = "",
    val boundingBoxes: String = "",
    val notes: String = ""
) {
    // Helper: get thumbnail (always first image)
    fun getThumbnailPath(): String {
        return try {
            val arr = JSONArray(imagePath)
            if (arr.length() > 0) arr.getString(0) else imagePath
        } catch (e: Exception) {
            imagePath // fallback for any legacy data
        }
    }

    // Helper: get all image paths as a list
    fun getAllImagePaths(): List<String> {
        return try {
            val arr = JSONArray(imagePath)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            listOf(imagePath) // fallback
        }
    }
}