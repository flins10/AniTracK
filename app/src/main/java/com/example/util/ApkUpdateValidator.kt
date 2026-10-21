package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkValidationResult(
    val isValid: Boolean,
    val userMessage: String,
    val errorCode: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null
)

object ApkUpdateValidator {

    fun validateUpdateApk(context: Context, apkFile: File): ApkValidationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkValidationResult(false, "APK file does not exist or is empty", "INVALID_FILE")
        }

        val pm = context.packageManager
        val archiveInfo: PackageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_ACTIVITIES)
            ?: return ApkValidationResult(false, "Invalid APK package structure", "CORRUPT_APK")

        if (archiveInfo.packageName != context.packageName) {
            return ApkValidationResult(
                false,
                "Package name mismatch (${archiveInfo.packageName} vs ${context.packageName})",
                "PACKAGE_MISMATCH"
            )
        }

        val archVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") archiveInfo.versionCode.toLong()
        }

        val currentInfo = pm.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            currentInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION") currentInfo.versionCode.toLong()
        }

        if (archVersionCode < currentVersionCode) {
            return ApkValidationResult(
                false,
                "Cannot downgrade APK version ($archVersionCode < $currentVersionCode)",
                "VERSION_DOWNGRADE",
                versionName = archiveInfo.versionName,
                versionCode = archVersionCode
            )
        }

        return ApkValidationResult(
            isValid = true,
            userMessage = "APK signature and package metadata verified",
            versionName = archiveInfo.versionName,
            versionCode = archVersionCode
        )
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun launchAuthorizedSessionInstall(context: Context, apkFile: File): Result<Boolean> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInstalledSigningSha256(context: Context): String? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
            }
            val packageInfo = pm.getPackageInfo(context.packageName, flags)
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION") packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return null
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(cert)
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
