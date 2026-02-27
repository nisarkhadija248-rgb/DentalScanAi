package com.dentalscan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relationship: String, // e.g. "Dad", "Mom", "Child"
    val age: Int,
    val imagePath: String = "" // optional profile picture
)