package dev.flightlink

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Opens a converted link in the matching app when it is installed.
 *
 * If the app is missing, the Play Store listing is opened instead of a web page:
 * the whole point of the utility is to hand the flight to the native app.
 */
object AppLauncher {

    const val SKYCARDS_PACKAGE = "com.flightradar24.skycards"

    val FR24_PACKAGES = listOf(
        "com.flightradar24free",
        "com.flightradar24pro",
        "com.flightradar24"
    )

    enum class Outcome {
        OPENED_APP,
        OPENED_BROWSER,
        STORE_REQUIRED,
        FAILED
    }

    data class Result(
        val outcome: Outcome,
        val packageName: String? = null
    )

    fun candidatesFor(source: Conversion.Source): List<String> = when (source) {
        Conversion.Source.FLIGHTRADAR24 -> FR24_PACKAGES
        Conversion.Source.SKYCARDS -> listOf(SKYCARDS_PACKAGE)
    }

    fun open(context: Context, conversion: Conversion): Result {
        return openUri(
            context = context,
            url = conversion.targetUrl,
            candidates = candidatesFor(conversion.source)
        )
    }

    fun openUri(context: Context, url: String, candidates: List<String>): Result {
        val uri = Uri.parse(url)
        val installed = candidates.filter { isInstalled(context, it) }

        if (installed.isEmpty()) {
            return openStore(context, candidates.firstOrNull())
        }

        for (packageName in installed) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                context.startActivity(intent)
                return Result(Outcome.OPENED_APP, packageName)
            } catch (_: ActivityNotFoundException) {
                // The app is installed but does not advertise this link; try
                // the generic handler below.
            } catch (_: SecurityException) {
                // Same as above.
            }
        }

        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return Result(Outcome.OPENED_BROWSER)
        } catch (_: ActivityNotFoundException) {
            // Fall through to the store.
        } catch (_: SecurityException) {
            // Fall through to the store.
        }

        return openStore(context, candidates.firstOrNull())
    }

    private fun openStore(context: Context, packageName: String?): Result {
        if (packageName == null) return Result(Outcome.FAILED)

        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(marketIntent)
            return Result(Outcome.STORE_REQUIRED, packageName)
        } catch (_: ActivityNotFoundException) {
            // Fall back to the web Play Store.
        } catch (_: SecurityException) {
            // Fall back to the web Play Store.
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(webIntent)
            Result(Outcome.STORE_REQUIRED, packageName)
        } catch (_: ActivityNotFoundException) {
            Result(Outcome.FAILED)
        } catch (_: SecurityException) {
            Result(Outcome.FAILED)
        }
    }

    @Suppress("DEPRECATION")
    private fun isInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
