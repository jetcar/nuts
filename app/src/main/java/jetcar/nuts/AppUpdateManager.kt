package jetcar.nuts

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class ReleaseData(
    val tagName: String,
    val changelog: String,
    val downloadUrl: String,
)

object AppUpdateManager {
    private const val latestReleaseUrl = "https://api.github.com/repos/jetcar/nuts/releases/latest"
    private const val preferencesName = "nuts_updates"
    private const val skippedVersionKey = "skipped_version"
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun checkForUpdate(context: Context, callback: (ReleaseData?) -> Unit) {
        val appContext = context.applicationContext

        backgroundExecutor.execute {
            val release = runCatching { fetchLatestRelease() }.getOrNull()
            val result = release?.takeIf {
                val remoteVersion = it.tagName.removePrefix("v")
                val skippedVersion = getSkippedVersion(appContext)?.removePrefix("v") ?: "0"
                AppVersioning.compareVersions(remoteVersion, getLocalVersion(appContext)) > 0 &&
                    AppVersioning.compareVersions(remoteVersion, skippedVersion) > 0
            }

            mainHandler.post {
                callback(result)
            }
        }
    }

    fun skipVersion(context: Context, versionTag: String) {
        context.applicationContext
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(skippedVersionKey, versionTag)
            .apply()
    }

    private fun getSkippedVersion(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(skippedVersionKey, null)
    }

    private fun getLocalVersion(context: Context): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        return packageInfo.versionName.orEmpty()
    }

    private fun fetchLatestRelease(): ReleaseData {
        val connection = (URL(latestReleaseUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "nuts-android")
        }

        try {
            val body = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw IllegalStateException("GitHub releases request failed: ${connection.responseCode}")
            }

            val json = JSONObject(body)
            return ReleaseData(
                tagName = json.getString("tag_name"),
                changelog = json.optString("body"),
                downloadUrl = findApkAssetUrl(json),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun findApkAssetUrl(json: JSONObject): String {
        val assets = json.getJSONArray("assets")

        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            val url = asset.optString("browser_download_url")
            if (url.endsWith(".apk", ignoreCase = true)) {
                return url
            }
        }

        if (assets.length() > 0) {
            return assets.getJSONObject(0).getString("browser_download_url")
        }

        throw IllegalStateException("Latest release has no downloadable assets")
    }
}
