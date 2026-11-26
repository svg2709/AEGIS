package com.aegis.survival.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aegis.survival.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
private data class GitHubAssetDto(
    val name: String = "",
    val size: Long = 0L,
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
)

data class AppUpdateInfo(
    val latestVersion: String,
    val tagName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val checksumUrl: String?,
    val releasePageUrl: String,
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}

class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(
        repoSlug: String = BuildConfig.GITHUB_REPO,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val cleanRepo = repoSlug.trim().trim('/')
        if (!REPO_SLUG_REGEX.matches(cleanRepo)) {
            return@withContext UpdateCheckResult.Failed("Invalid GitHub repository format.")
        }

        val endpoint = "https://api.github.com/repos/$cleanRepo/releases/latest"
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AEGIS-Android/${BuildConfig.VERSION_NAME}")
            }

            val code = conn.responseCode
            if (code == 404) {
                return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Failed("GitHub returned HTTP $code")
            }

            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val release = json.decodeFromString<GitHubReleaseDto>(body)
            val latestVer = release.tagName.removePrefix("v").trim()
            val currentVer = BuildConfig.VERSION_NAME.substringBefore("-").trim()

            if (latestVer.isEmpty() || !isNewerVersion(latestVer, currentVer)) {
                return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
            }

            val apkAsset = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) &&
                    !asset.name.contains("-debug", ignoreCase = true)
            } ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

            if (apkAsset == null || !isTrustedGitHubUrl(apkAsset.browserDownloadUrl)) {
                return@withContext UpdateCheckResult.Failed("Release $latestVer has no verified APK asset.")
            }

            val checksumAsset = release.assets.firstOrNull {
                it.name.equals("SHA256SUMS.txt", ignoreCase = true) &&
                    isTrustedGitHubUrl(it.browserDownloadUrl)
            }

            UpdateCheckResult.UpdateAvailable(
                AppUpdateInfo(
                    latestVersion = latestVer,
                    tagName = release.tagName,
                    releaseTitle = release.name.ifBlank { "AEGIS ${release.tagName}" },
                    changelog = release.body.trim().ifBlank { "New survival protocols and improvements." },
                    apkName = apkAsset.name,
                    apkUrl = apkAsset.browserDownloadUrl,
                    apkSizeBytes = apkAsset.size,
                    checksumUrl = checksumAsset?.browserDownloadUrl,
                    releasePageUrl = release.htmlUrl,
                )
            )
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Unable to reach GitHub Releases")
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun downloadAndVerifyApk(
        info: AppUpdateInfo,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!isTrustedGitHubUrl(info.apkUrl)) {
            return@withContext Result.failure(SecurityException("Blocked untrusted download host"))
        }

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "aegis-update-${info.latestVersion}.apk")
        if (targetFile.exists()) targetFile.delete()

        try {
            val expectedSha = info.checksumUrl?.let { fetchExpectedSha256(it, info.apkName) }

            val conn = openTrustedRedirectConnection(info.apkUrl)
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    return@withContext Result.failure(IllegalStateException("Download failed (HTTP $code)"))
                }

                val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: info.apkSizeBytes
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0L) {
                                onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                        output.flush()
                    }
                }

                if (expectedSha != null) {
                    val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                        targetFile.delete()
                        return@withContext Result.failure(
                            SecurityException("APK SHA-256 checksum mismatch — installation cancelled for safety.")
                        )
                    }
                }

                onProgress(1f)
                Result.success(targetFile)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            targetFile.delete()
            Result.failure(e)
        }
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchApkInstaller(apkFile: File): Result<Unit> {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchExpectedSha256(checksumUrl: String, apkName: String): String? {
        if (!isTrustedGitHubUrl(checksumUrl)) return null
        val conn = openTrustedRedirectConnection(checksumUrl)
        return try {
            if (conn.responseCode !in 200..299) return null
            val content = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            content.lines()
                .map { it.trim() }
                .firstOrNull { it.endsWith(apkName) }
                ?.split(Regex("\\s+"))
                ?.firstOrNull()
                ?.takeIf { it.length == 64 }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun openTrustedRedirectConnection(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects < 5) {
            if (!isTrustedGitHubUrl(currentUrl)) {
                throw SecurityException("Blocked redirect to untrusted host")
            }
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "AEGIS-Android/${BuildConfig.VERSION_NAME}")
            }
            val code = conn.responseCode
            if (code in 301..308) {
                val nextLocation = conn.getHeaderField("Location")
                conn.disconnect()
                if (nextLocation.isNullOrBlank()) {
                    throw IllegalStateException("Empty redirect location")
                }
                currentUrl = nextLocation
                redirects++
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while downloading update")
    }

    private fun isTrustedGitHubUrl(rawUrl: String): Boolean {
        return try {
            val uri = URI(rawUrl.trim())
            val host = uri.host?.lowercase() ?: return false
            uri.scheme.equals("https", ignoreCase = true) && (
                host == "github.com" ||
                    host == "api.github.com" ||
                    host == "objects.githubusercontent.com" ||
                    host == "release-assets.githubusercontent.com" ||
                    host.endsWith(".githubusercontent.com")
                )
        } catch (_: Exception) {
            false
        }
    }

    fun isNewerVersion(candidate: String, current: String): Boolean {
        val cParts = candidate.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val curParts = current.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(cParts.size, curParts.size)
        for (i in 0 until maxLen) {
            val a = cParts.getOrElse(i) { 0 }
            val b = curParts.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    companion object {
        private val REPO_SLUG_REGEX = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    }
}
