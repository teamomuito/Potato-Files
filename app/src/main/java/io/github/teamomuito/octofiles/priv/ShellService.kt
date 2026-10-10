package io.github.teamomuito.octofiles.priv

/**
 * Runs inside Shizuku's own process, which has shell rights (the same ones as adb). Shizuku creates
 * it by class name, so it's kept out of R8's reach in proguard-rules.pro.
 */
class ShellService : IShellService.Stub() {

    /** The first line is the exit code, the rest is everything the command printed, errors included. */
    override fun exec(command: String): String {
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        process.outputStream.close()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        return "${process.waitFor()}\n$text"
    }

    override fun destroy() {
        System.exit(0)
    }
}
