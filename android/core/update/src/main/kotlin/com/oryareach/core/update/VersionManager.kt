package com.oryareach.core.update

import android.content.Context
import android.content.pm.PackageManager

/**
 * Reads this device's installed version rather than depending on `:app`'s `BuildConfig` —
 * `:core:update` would otherwise have to depend on the module that depends on it.
 */
class VersionManager(private val context: Context) {

    fun currentVersionName(): String =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            ?: FALLBACK

    private companion object {
        const val FALLBACK = "0.0.0"
    }
}
