package com.dentalscan.utils

import com.dentalscan.data.ScanEntity
import java.io.File

object ScanFiles {

    /**
     * Deletes the photo files of a scan. Only files the camera created ("dental_scan_step..."),
     * so a wrong or old path can never remove anything else. Returns how many files were deleted.
     */
    fun deletePhotos(scan: ScanEntity): Int =
        scan.getAllImagePaths()
            .filter { it.isNotEmpty() }
            .count { path ->
                val file = File(path)
                file.isFile && file.name.startsWith("dental_scan_") && file.delete()
            }
}
