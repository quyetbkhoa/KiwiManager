package com.kiwi.manager.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.kiwi.manager.domain.model.InstalledVersionInfo

class PackageManagerWrapper(private val context: Context) {

    fun getInstalledVersion(packageName: String): InstalledVersionInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            InstalledVersionInfo(
                packageName = packageName,
                versionName = packageInfo.versionName ?: "",
                versionCode = versionCode,
                isInstalled = true
            )
        } catch (e: PackageManager.NameNotFoundException) {
            InstalledVersionInfo(
                packageName = packageName,
                versionName = "",
                versionCode = 0,
                isInstalled = false
            )
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return getInstalledVersion(packageName).isInstalled
    }

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getLaunchIntent(packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }
}
