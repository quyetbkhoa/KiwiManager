package com.kiwi.manager.util

object VersionComparator {
    fun compare(v1: String, v2: String): Int {
        val clean1 = cleanVersion(v1)
        val clean2 = cleanVersion(v2)
        if (clean1 == clean2) return 0

        val parts1 = clean1.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = clean2.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    fun isNewer(newVersion: String, currentVersion: String): Boolean {
        return compare(newVersion, currentVersion) > 0
    }

    fun computeVersionCode(versionName: String): Int {
        val clean = cleanVersion(versionName)
        val parts = clean.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val major = parts.getOrNull(0) ?: 0
        val minor = parts.getOrNull(1) ?: 0
        val patch = parts.getOrNull(2) ?: 0
        return major * 10000 + minor * 100 + patch
    }

    fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }
}

