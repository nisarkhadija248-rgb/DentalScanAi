package com.dentalscan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert
    suspend fun insertScan(scan: ScanEntity): Long

    @Query("SELECT * FROM scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun getScansForUser(userId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanById(scanId: Long): ScanEntity?

    @Query("SELECT * FROM scans WHERE userId = :userId AND condition = :condition ORDER BY timestamp DESC")
    fun getScansByCondition(userId: String, condition: String): Flow<List<ScanEntity>>

    /** Replaces the analysis of a saved scan (after it was analysed again), and its photos if some were cropped. */
    @Query(
        "UPDATE scans SET imagePath = :imagePath, condition = :condition, confidence = :confidence, " +
            "severity = :severity, boundingBoxes = :boundingBoxes, checkedSteps = :checkedSteps WHERE id = :scanId"
    )
    suspend fun updateAnalysis(
        scanId: Long,
        imagePath: String,
        condition: String,
        confidence: Float,
        severity: String,
        boundingBoxes: String,
        checkedSteps: String?
    )

    @Query("UPDATE scans SET checkedSteps = :checkedSteps WHERE id = :scanId")
    suspend fun updateCheckedSteps(scanId: Long, checkedSteps: String?)

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("SELECT DISTINCT userId, userName FROM scans")
    suspend fun getAllUsers(): List<UserProfile>

    @Query("SELECT COUNT(*) FROM scans WHERE userId = :userId")
    suspend fun getScanCountForUser(userId: String): Int
}