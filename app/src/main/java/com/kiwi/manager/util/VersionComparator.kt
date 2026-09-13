package com.kiwi.manager.util

object VersionComparator {
    fun isNewer(newVersion: String, currentVersion: String): Boolean {
        val newCode = computeVersionCode(cleanVersion(newVersion))
        val currentCode = computeVersionCode(cleanVersion(currentVersion))
        return newCode > currentCode
    }

    fun computeVersionCode(versionName: String): Int {
        val clean = cleanVersion(versionName)
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        val major = parts.getOrNull(0) ?: 0
        val minor = parts.getOrNull(1) ?: 0
        val patch = parts.getOrNull(2) ?: 0
        return major * 10000 + minor * 100 + patch
    }

    fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}
