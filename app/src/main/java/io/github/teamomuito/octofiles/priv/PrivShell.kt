package io.github.teamomuito.octofiles.priv

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Process
import io.github.teamomuito.octofiles.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

enum class ShizukuState { MISSING, NEEDS_PERMISSION, OUTDATED, READY }

/**
 * Shell commands with adb's rights, through Shizuku. Nothing here needs root. Potato only binds
 * to Shizuku when a section that needs it is opened.
 */
object PrivShell {
    const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"
    private const val REQUEST_CODE = 7301
    private const val BIND_TIMEOUT_MS = 10_000L
    private const val USER_UID_BLOCK = 100_000

    private val lock = Mutex()
    @Volatile private var service: IShellService? = null

    fun state(): ShizukuState = when {
        !runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> ShizukuState.MISSING
        Shizuku.isPreV11() -> ShizukuState.OUTDATED
        Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuState.NEEDS_PERMISSION
        else -> ShizukuState.READY
    }

    fun askPermission() = Shizuku.requestPermission(REQUEST_CODE)

    /** Calls [onChange] when Shizuku starts, stops, or answers the permission prompt. Returns the way to stop listening. */
    fun listen(onChange: () -> Unit): () -> Unit {
        val permission = Shizuku.OnRequestPermissionResultListener { _, _ -> onChange() }
        val received = Shizuku.OnBinderReceivedListener { onChange() }
        val dead = Shizuku.OnBinderDeadListener {
            service = null
            onChange()
        }
        Shizuku.addRequestPermissionResultListener(permission)
        Shizuku.addBinderReceivedListener(received)
        Shizuku.addBinderDeadListener(dead)
        return {
            Shizuku.removeRequestPermissionResultListener(permission)
            Shizuku.removeBinderReceivedListener(received)
            Shizuku.removeBinderDeadListener(dead)
        }
    }

    /** Runs one command as shell. Output includes stderr. Fails if Shizuku isn't ready or drops mid-call. */
    suspend fun exec(context: Context, command: String): ShellOutput = withContext(Dispatchers.IO) {
        check(state() == ShizukuState.READY) { "shizuku isn't ready" }
        val raw = try {
            shell(context).exec(command)
        } catch (e: Exception) {
            service = null
            throw IllegalStateException("shizuku dropped the connection", e)
        }
        ShellText.parseExec(raw)
    }

    /** Every package on the phone, including ones kept as data after an uninstall. Fails rather than guessing with an empty list. */
    suspend fun installed(context: Context): Set<String> {
        // scoped to the user this app runs as, since that's whose storage is being scanned.
        // Android gives every user its own block of uids, so the user id is the uid divided by the block size.
        val out = exec(context, "pm list packages -u --user ${Process.myUid() / USER_UID_BLOCK}")
        val names = ShellText.parsePackages(out.text)
        check(out.ok && names.isNotEmpty()) { "shizuku couldn't list the installed apps" }
        return names
    }

    /** Asks Android to trim every app's cache as far as it goes. Only caches are touched, never app data. */
    suspend fun trimCaches(context: Context): ShellOutput = exec(context, "pm trim-caches 1000G")

    private suspend fun shell(context: Context): IShellService = lock.withLock {
        service?.takeIf { it.asBinder().isBinderAlive }?.let { return@withLock it }
        val bound = CompletableDeferred<IShellService>()
        val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
        Shizuku.bindUserService(args, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                bound.complete(IShellService.Stub.asInterface(binder))
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
            }
        })
        val connected = withTimeoutOrNull(BIND_TIMEOUT_MS) { bound.await() }
            ?: throw IllegalStateException("shizuku didn't answer in time")
        service = connected
        connected
    }
}
