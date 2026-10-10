package io.github.teamomuito.octofiles.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.teamomuito.octofiles.data.Access
import io.github.teamomuito.octofiles.data.ClearAssist
import io.github.teamomuito.octofiles.data.AppRules
import io.github.teamomuito.octofiles.data.AppUsage
import io.github.teamomuito.octofiles.data.Apps
import io.github.teamomuito.octofiles.data.Cleaner
import io.github.teamomuito.octofiles.data.JunkItem
import io.github.teamomuito.octofiles.data.JunkKind
import io.github.teamomuito.octofiles.data.JunkReport
import io.github.teamomuito.octofiles.data.JunkRules
import io.github.teamomuito.octofiles.data.Prefs
import io.github.teamomuito.octofiles.files.Fs
import io.github.teamomuito.octofiles.priv.Corpse
import io.github.teamomuito.octofiles.priv.Corpses
import io.github.teamomuito.octofiles.priv.DeepCache
import io.github.teamomuito.octofiles.priv.ExternalCache
import io.github.teamomuito.octofiles.priv.PrivShell
import io.github.teamomuito.octofiles.priv.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val files: Int) : ScanState
    data class Done(val report: JunkReport) : ScanState
}

/** A look through something that needs Shizuku: not started, working, what it found, or why it couldn't look. */
sealed interface PrivScan<out T> {
    data object Idle : PrivScan<Nothing>
    data object Working : PrivScan<Nothing>
    data class Done<T>(val items: List<T>) : PrivScan<T>
    data class Failed(val reason: String) : PrivScan<Nothing>
}

private const val GUIDED_MIN_BYTES = 5L * 1024 * 1024

class CleanViewModel(app: Application) : AndroidViewModel(app) {

    val hasAllFiles = MutableStateFlow(Access.hasAllFiles())
    val hasUsage = MutableStateFlow(Access.hasUsageAccess(app))
    val scan = MutableStateFlow<ScanState>(ScanState.Idle)

    /** Installed apps with their sizes, or null until loaded (or without usage access). */
    val apps = MutableStateFlow<List<AppUsage>?>(null)

    /** Paths ticked for deletion. */
    val selected = MutableStateFlow<Set<String>>(emptySet())
    val working = MutableStateFlow(false)
    val cleaned: StateFlow<Long> = Prefs.cleaned

    private var scanJob: Job? = null

    // shizuku: the deeper clean, without root. Only looked at when one of its sections is opened.
    val shizuku = MutableStateFlow(ShizukuState.MISSING)
    val corpses = MutableStateFlow<PrivScan<Corpse>>(PrivScan.Idle)
    val corpsePicked = MutableStateFlow<Set<String>>(emptySet())
    val externalCaches = MutableStateFlow<PrivScan<ExternalCache>>(PrivScan.Idle)
    val cachePicked = MutableStateFlow<Set<String>>(emptySet())
    val privBusy = MutableStateFlow(false)
    private val stopShizukuListener = PrivShell.listen { refreshShizuku() }

    fun refresh() {
        val app = getApplication<Application>()
        hasAllFiles.value = Access.hasAllFiles()
        hasUsage.value = Access.hasUsageAccess(app)
        refreshShizuku()
        if (hasAllFiles.value && scan.value == ScanState.Idle) scan()
        if (hasUsage.value && apps.value == null) loadApps()
    }

    fun scan() {
        if (!Access.hasAllFiles()) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            scan.value = ScanState.Scanning(0)
            val root = Cleaner.scan { seen -> scan.value = ScanState.Scanning(seen) }
            val report = JunkRules.find(root, System.currentTimeMillis())
            // the safe stuff starts ticked; large files are personal, so nothing there is ticked for you
            selected.value = report.items.filter { it.kind != JunkKind.LARGE }.mapTo(HashSet()) { it.path }
            scan.value = ScanState.Done(report)
        }
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            apps.value = if (Access.hasUsageAccess(getApplication())) Apps.load(getApplication()) else null
        }
    }

    fun toggle(item: JunkItem) = selected.update { if (item.path in it) it - item.path else it + item.path }

    fun setAll(items: List<JunkItem>, on: Boolean) = selected.update { current ->
        val paths = items.map { it.path }
        if (on) current + paths else current - paths.toSet()
    }

    /** Deletes everything ticked, adds it to the running total, then looks again. Returns bytes freed. */
    suspend fun clean(): Long {
        val report = (scan.value as? ScanState.Done)?.report ?: return 0
        val picked = report.items.filter { it.path in selected.value }
        if (picked.isEmpty()) return 0
        working.value = true
        val freed = withContext(Dispatchers.IO) { Cleaner.delete(picked) }
        Prefs.addCleaned(freed)
        working.value = false
        scan()
        return freed
    }

    /** After Android's clear-all-caches screen: whatever the cache total dropped by was freed. */
    suspend fun recountCaches(before: Long): Long = withContext(Dispatchers.IO) {
        val now = Apps.load(getApplication())
        apps.value = now
        (before - now.sumOf { it.cacheBytes }).coerceAtLeast(0).also { Prefs.addCleaned(it) }
    }

    /**
     * One by one: for phones without Android's clear-all screen (Samsung, for one). Each app's
     * settings page gets opened in turn, biggest cache first; the person taps clear cache there.
     */
    data class Guided(val queue: List<AppUsage>, val done: Int = 0, val freed: Long = 0) {
        val next: AppUsage? get() = queue.firstOrNull()
    }

    val guided = MutableStateFlow<Guided?>(null)

    fun startGuided() {
        val withCache = AppRules.byCache(apps.value.orEmpty()).filter { it.cacheBytes > 0 }
        // skip the crumbs, unless crumbs are all there is
        val worth = withCache.filter { it.cacheBytes >= GUIDED_MIN_BYTES }.ifEmpty { withCache }
        guided.value = if (worth.isEmpty()) null else Guided(worth)
    }

    fun skipGuided() = guided.update { g -> g?.let { if (it.queue.size <= 1) null else it.copy(queue = it.queue.drop(1)) } }

    fun stopGuided() {
        guided.value = null
        loadApps()
    }

    /** Back from an app's settings page: count what went, move on to the next one. */
    fun afterGuidedStep() {
        ClearAssist.end()
        val g = guided.value ?: return
        val app = g.next ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val after = Apps.cacheOf(getApplication(), app.pkg) ?: 0
            val freed = (app.cacheBytes - after).coerceAtLeast(0)
            Prefs.addCleaned(freed)
            apps.update { list -> list?.map { if (it.pkg == app.pkg) it.copy(cacheBytes = after) else it } }
            guided.update { current ->
                current?.let { it.copy(queue = it.queue.drop(1), done = it.done + if (freed > 0) 1 else 0, freed = it.freed + freed) }
            }
        }
    }

    fun afterUninstall(pkg: String) {
        if (!Apps.isInstalled(getApplication(), pkg)) {
            apps.update { list -> list?.filter { it.pkg != pkg } }
        }
    }

    fun refreshShizuku() {
        shizuku.value = PrivShell.state()
    }

    fun askShizuku() = PrivShell.askPermission()

    fun scanCorpses() {
        if (shizuku.value != ShizukuState.READY || corpses.value == PrivScan.Working) return
        corpses.value = PrivScan.Working
        viewModelScope.launch {
            corpses.value = runCatching { Corpses.scan(getApplication(), storageRoot()) }.fold(
                onSuccess = { found ->
                    // app data starts ticked, the rest is for the person to choose
                    corpsePicked.value = found.filter { it.kind.ticked }.mapTo(HashSet()) { it.path }
                    PrivScan.Done(found)
                },
                onFailure = { PrivScan.Failed(reasonOf(it)) },
            )
        }
    }

    fun toggleCorpse(corpse: Corpse) = corpsePicked.update { if (corpse.path in it) it - corpse.path else it + corpse.path }

    fun setCorpses(items: List<Corpse>, on: Boolean) = corpsePicked.update { current ->
        val paths = items.map { it.path }
        if (on) current + paths else current - paths.toSet()
    }

    /** Removes the ticked leftovers, adds them to the running total, then looks again. */
    suspend fun removeCorpses(): Result<Long> {
        val found = (corpses.value as? PrivScan.Done<Corpse>)?.items.orEmpty()
        val picked = found.filter { it.path in corpsePicked.value }
        if (picked.isEmpty()) return Result.success(0L)
        privBusy.value = true
        val result = runCatching { Corpses.remove(getApplication(), storageRoot(), picked) }
        result.onSuccess { Prefs.addCleaned(it) }
        privBusy.value = false
        scanCorpses()
        return result
    }

    fun scanCaches() {
        if (shizuku.value != ShizukuState.READY || externalCaches.value == PrivScan.Working) return
        externalCaches.value = PrivScan.Working
        viewModelScope.launch {
            externalCaches.value = runCatching {
                withContext(Dispatchers.IO) { DeepCache.scan(getApplication(), storageRoot()).map { labelled(it) } }
            }.fold(
                onSuccess = { found ->
                    // caches are safe to clear, so they all start ticked
                    cachePicked.value = found.mapTo(HashSet()) { it.path }
                    PrivScan.Done(found)
                },
                onFailure = { PrivScan.Failed(reasonOf(it)) },
            )
        }
    }

    fun toggleCache(cache: ExternalCache) = cachePicked.update { if (cache.path in it) it - cache.path else it + cache.path }

    fun setCaches(items: List<ExternalCache>, on: Boolean) = cachePicked.update { current ->
        val paths = items.map { it.path }
        if (on) current + paths else current - paths.toSet()
    }

    /** Clears the ticked app cache folders. Returns the bytes freed. */
    suspend fun clearPickedCaches(): Result<Long> {
        val found = (externalCaches.value as? PrivScan.Done<ExternalCache>)?.items.orEmpty()
        val picked = found.filter { it.path in cachePicked.value }
        if (picked.isEmpty()) return Result.success(0L)
        privBusy.value = true
        val result = runCatching { DeepCache.clear(getApplication(), picked, storageRoot()) }
        result.onSuccess { Prefs.addCleaned(it) }
        privBusy.value = false
        scanCaches()
        return result
    }

    /** Android's own trim of every app's cache, asked for through Shizuku. Returns what the cache total dropped by. */
    suspend fun trimAllCaches(): Result<Long> {
        val before = apps.value.orEmpty().sumOf { it.cacheBytes }
        privBusy.value = true
        val result = runCatching {
            val out = PrivShell.trimCaches(getApplication())
            check(out.ok) { out.text.trim().ifEmpty { "shizuku couldn't trim the caches" } }
            recountCaches(before)
        }
        privBusy.value = false
        scanCaches()
        return result
    }

    override fun onCleared() {
        stopShizukuListener()
    }

    private fun storageRoot(): String = Fs.storage.absolutePath

    private fun reasonOf(e: Throwable): String = e.message ?: "shizuku didn't answer"

    @Suppress("DEPRECATION")
    private fun labelled(cache: ExternalCache): ExternalCache {
        val pm = getApplication<Application>().packageManager
        val label = runCatching { pm.getApplicationInfo(cache.pkg, 0).loadLabel(pm).toString() }.getOrDefault(cache.pkg)
        return cache.copy(label = label)
    }
}
