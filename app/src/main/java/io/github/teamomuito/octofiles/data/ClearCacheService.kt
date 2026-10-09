package io.github.teamomuito.octofiles.data

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Does the taps in the one-by-one walk for the person: Storage, then Clear cache, then back to
 * potato. It acts only while [ClearAssist] is active, and only inside the Settings app. It never
 * taps Clear storage, which would wipe the app's data.
 */
class ClearCacheService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!ClearAssist.active || event.packageName?.toString() != SETTINGS) return
        val root = rootInActiveWindow ?: return

        val clear = find(root) { it.text?.toString()?.trim().equals("Clear cache", ignoreCase = true) }
        if (clear != null) {
            // the walk moves on either way: a cache that's already empty just gets skipped
            ClearAssist.end()
            if (clear.isEnabled) clickable(clear)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, BACK_DELAY_MS)
        } else if (!ClearAssist.storageTapped) {
            val storage = find(root) { node -> STORAGE_LABELS.any { node.text?.toString()?.trim().equals(it, ignoreCase = true) } }
            val row = storage?.let { clickable(it) } ?: return
            ClearAssist.storageTapped = true
            row.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    override fun onInterrupt() {}

    private fun find(node: AccessibilityNodeInfo, matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (matches(node)) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            find(child, matches)?.let { return it }
        }
        return null
    }

    /** The node itself if it can be tapped, otherwise the closest parent that can. Labels often sit inside a tappable row. */
    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        while (n != null) {
            if (n.isClickable) return n
            n = n.parent
        }
        return null
    }

    private companion object {
        const val SETTINGS = "com.android.settings"
        const val BACK_DELAY_MS = 700L
        val STORAGE_LABELS = listOf("Storage & cache", "Storage and cache", "Storage")
    }
}
