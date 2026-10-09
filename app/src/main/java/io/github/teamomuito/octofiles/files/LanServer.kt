package io.github.teamomuito.octofiles.files

import java.io.File
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Read-only browser and downloads over the local Wi-Fi. Every request needs the token shown on the phone.
 * It's plain HTTP, so only use it on a network you trust.
 */
class LanServer(private val root: File, private val token: String) {

    private var server: ServerSocket? = null

    @Volatile
    private var running = false

    val isRunning: Boolean get() = running

    fun start(port: Int = PORT) {
        if (running) return
        val socket = ServerSocket(port)
        server = socket
        running = true
        thread(name = "octo-lan", isDaemon = true) {
            while (running) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                thread(isDaemon = true) { client.use { handle(it) } }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
    }

    private fun handle(socket: Socket) {
        socket.soTimeout = 10_000
        val reader = socket.getInputStream().bufferedReader()
        val requestLine = reader.readLine() ?: return
        while (true) {
            val header = reader.readLine() ?: break
            if (header.isEmpty()) break
        }
        val out = socket.getOutputStream()
        val parts = requestLine.split(" ")
        if (parts.size < 2 || parts[0] != "GET") {
            return text(out, "405 Method Not Allowed", "method not allowed")
        }
        val route = parts[1].substringBefore('?')
        val params = parseQuery(parts[1].substringAfter('?', ""))
        if (params["token"] != token) return text(out, "403 Forbidden", "add ?token=... from the phone screen")

        val rootPath = root.canonicalPath
        val target = File(root, params["path"].orEmpty().trimStart('/')).canonicalFile
        if (target.path != rootPath && !target.path.startsWith(rootPath + File.separator)) {
            return text(out, "403 Forbidden", "outside the shared folder")
        }
        if (!target.exists()) return text(out, "404 Not Found", "not found")

        when {
            route == "/file" && target.isFile -> sendFile(out, target)
            target.isDirectory -> sendListing(out, target, params["token"].orEmpty())
            else -> text(out, "400 Bad Request", "not a folder")
        }
    }

    private fun sendListing(out: OutputStream, dir: File, token: String) {
        val rel = dir.canonicalPath.removePrefix(root.canonicalPath).trimStart(File.separatorChar)
        val rows = Fs.sorted(dir.listFiles().orEmpty().filter { !it.isHidden }.map { it.toItem() }, SortBy.NAME)
            .joinToString("\n") { item ->
                val childRel = listOf(rel, item.name).filter { it.isNotEmpty() }.joinToString("/")
                val link = if (item.isDir) {
                    "/?token=${enc(token)}&path=${enc(childRel)}"
                } else {
                    "/file?token=${enc(token)}&path=${enc(childRel)}"
                }
                "<li><a href=\"$link\">${esc(item.name)}${if (item.isDir) "/" else ""}</a> <span>${if (item.isDir) "" else "${item.size} bytes"}</span></li>"
            }
        val body = """
            <!doctype html><html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>octo files</title>
            <style>body{font-family:sans-serif;margin:16px}li{margin:6px 0}span{color:#777;font-size:.85em}</style>
            </head><body><h3>/${esc(rel)}</h3><ul>$rows</ul></body></html>
        """.trimIndent()
        val bytes = body.toByteArray(Charsets.UTF_8)
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun sendFile(out: OutputStream, file: File) {
        val name = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
        out.write(
            ("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\n" +
                "Content-Disposition: attachment; filename*=UTF-8''$name\r\n" +
                "Content-Length: ${file.length()}\r\nConnection: close\r\n\r\n").toByteArray(),
        )
        file.inputStream().use { it.copyTo(out) }
        out.flush()
    }

    private fun text(out: OutputStream, status: String, message: String) {
        val bytes = message.toByteArray(Charsets.UTF_8)
        out.write("HTTP/1.1 $status\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun parseQuery(query: String): Map<String, String> = query
        .split('&')
        .filter { it.contains('=') }
        .associate { it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), "UTF-8") }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    companion object {
        const val PORT = 8080

        fun newToken(): String = (100000..999999).random().toString()
    }
}
