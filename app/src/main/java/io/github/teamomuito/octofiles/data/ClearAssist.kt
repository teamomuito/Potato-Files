package io.github.teamomuito.octofiles.data

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/**
 * State for the one-by-one cache walk. The helper only taps while a walk has an app's settings
 * page open, so it stays quiet the rest of the time.
 */
object ClearAssist {
    @Volatile var active = false
    @Volatile var storageTapped = false

    fun begin() {
        storageTapped = false
        active = true
    }

    fun end() {
        active = false
    }

    /** Whether the person has switched the helper on in Android's accessibility settings. */
    fun enabled(context: Context): Boolean {
        val on = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val me = ComponentName(context, ClearCacheService::class.java).flattenToString()
        return on.split(':').any { it.equals(me, ignoreCase = true) }
    }
}
