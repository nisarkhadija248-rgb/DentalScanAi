package com.dentalscan.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String, // e.g. "Dad", "Mom", "Child"
    val age: Int,
    val imagePath: String = "", // optional profile picture
    // Dental Health Score 0..100. Always recomputed from the profile's scans (see HealthScore);
    // this column is the saved copy. 100 until the first scan.
    @ColumnInfo(defaultValue = "100")
    val healthScore: Int = 100
)