package io.github.teamomuito.octofiles.files

import java.io.File

const val DAY = 24L * 60 * 60 * 1000

enum class FileKind(val label: String) {
    FOLDER("folder"), IMAGE("image"), VIDEO("video"), AUDIO("audio"),
    DOCUMENT("document"), ARCHIVE("archive"), APK("app"), TEXT("text"), OTHER("file"),
}

private val extKinds: Map<String, FileKind> = buildMap {
    listOf("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "dng").forEach { put(it, FileKind.IMAGE) }
    listOf("mp4", "mkv", "mov", "3gp", "webm").forEach { put(it, FileKind.VIDEO) }
    listOf("mp3", "m4a", "wav", "ogg", "flac").forEach { put(it, FileKind.AUDIO) }
    listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt").forEach { put(it, FileKind.DOCUMENT) }
    listOf("zip", "7z", "rar", "tar", "gz").forEach { put(it, FileKind.ARCHIVE) }
    put("apk", FileKind.APK)
    listOf("txt", "md", "csv", "json", "xml", "log", "kt", "java").forEach { put(it, FileKind.TEXT) }
}

fun kindOf(name: String, isDir: Boolean): FileKind {
    if (isDir) return FileKind.FOLDER
    return extKinds[name.substringAfterLast('.', "").lowercase()] ?: FileKind.OTHER
}

data class FileItem(
    val file: File,
    val kind: FileKind,
    val size: Long,
    val modified: Long,
) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
    val isDir: Boolean get() = kind == FileKind.FOLDER
}

fun File.toItem(): FileItem {
    val dir = isDirectory
    return FileItem(this, kindOf(name, dir), if (dir) 0L else length(), lastModified())
}
