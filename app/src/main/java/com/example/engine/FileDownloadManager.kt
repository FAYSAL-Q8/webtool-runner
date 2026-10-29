/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.engine

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object FileDownloadManager {

    private const val FOLDER_NAME = "ToolCraft"

    private data class ActiveStream(
        val streamId: String,
        val filename: String,
        val mimeType: String,
        val outputStream: OutputStream,
        val mediaStoreUri: Uri?,
        val file: File?,
        var bytesWritten: Long = 0,
        val totalExpectedBytes: Long = 0
    )

    private val activeStreams = ConcurrentHashMap<String, ActiveStream>()

    /**
     * Starts a chunked streaming download session for large files (BLOBs, generated art, large exports).
     * Bypasses Android WebView IPC message size limits by receiving chunks sequentially.
     */
    fun startStreamDownload(
        context: Context,
        streamId: String,
        filename: String,
        mimeType: String?,
        totalExpectedBytes: Long = 0
    ): Boolean {
        try {
            val cleanFilename = sanitizeFilename(filename, mimeType)
            val effectiveMime = if (mimeType.isNullOrBlank() || mimeType == "application/octet-stream") {
                guessMimeType(cleanFilename)
            } else {
                mimeType
            }

            var outputStream: OutputStream? = null
            var savedUri: Uri? = null
            var targetFile: File? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, cleanFilename)
                    put(MediaStore.Downloads.MIME_TYPE, effectiveMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("Failed to create MediaStore entry")
                outputStream = resolver.openOutputStream(uri)
                    ?: throw Exception("Failed to open MediaStore output stream")
                savedUri = uri
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, FOLDER_NAME)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val uniqueFile = getUniqueFile(targetDir, cleanFilename)
                targetFile = uniqueFile
                outputStream = FileOutputStream(uniqueFile)
            }

            val session = ActiveStream(
                streamId = streamId,
                filename = cleanFilename,
                mimeType = effectiveMime,
                outputStream = outputStream,
                mediaStoreUri = savedUri,
                file = targetFile,
                totalExpectedBytes = totalExpectedBytes
            )
            activeStreams[streamId] = session

            CoroutineScope(Dispatchers.Main).launch {
                val sizeInfo = if (totalExpectedBytes > 0) " (${formatFileSize(totalExpectedBytes)})" else ""
                Toast.makeText(
                    context,
                    "Downloading $cleanFilename$sizeInfo to Downloads/$FOLDER_NAME...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Cannot start download: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }

    /**
     * Appends a Base64-encoded chunk to an active stream.
     * Decodes and streams directly to disk without keeping entire file in memory.
     */
    fun appendStreamChunk(streamId: String, base64Chunk: String): Boolean {
        val session = activeStreams[streamId] ?: return false
        return try {
            if (base64Chunk.isNotEmpty()) {
                val chunkBytes = Base64.decode(base64Chunk, Base64.DEFAULT)
                session.outputStream.write(chunkBytes)
                session.bytesWritten += chunkBytes.size
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Finalizes a chunked streaming download session, flushes disk buffers, and notifies MediaScanner.
     */
    fun finishStreamDownload(
        context: Context,
        streamId: String,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ): Boolean {
        val session = activeStreams.remove(streamId) ?: return false
        return try {
            session.outputStream.flush()
            session.outputStream.close()

            var finalUri = session.mediaStoreUri
            var finalPath = "Downloads/$FOLDER_NAME/${session.filename}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && session.mediaStoreUri != null) {
                val resolver = context.contentResolver
                val updateValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(session.mediaStoreUri, updateValues, null, null)
            } else if (session.file != null) {
                finalPath = session.file.absolutePath
                finalUri = Uri.fromFile(session.file)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(session.file.absolutePath),
                    arrayOf(session.mimeType),
                    null
                )
            }

            val formattedSize = formatFileSize(session.bytesWritten)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "Saved ${session.filename} ($formattedSize) to Downloads/$FOLDER_NAME",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(true, finalPath, finalUri)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false, e.message ?: "Save error", null)
            }
            false
        }
    }

    /**
     * Cancels an active stream download and cleans up partial file.
     */
    fun cancelStreamDownload(context: Context, streamId: String) {
        val session = activeStreams.remove(streamId) ?: return
        try {
            session.outputStream.close()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && session.mediaStoreUri != null) {
                context.contentResolver.delete(session.mediaStoreUri, null, null)
            } else {
                session.file?.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Streams an InputStream directly into Downloads/ToolCraft/ using 64KB buffer.
     * Prevents OutOfMemoryError regardless of file size.
     */
    fun saveStream(
        context: Context,
        inputStream: InputStream,
        filename: String,
        mimeType: String?,
        totalBytes: Long = 0,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cleanFilename = sanitizeFilename(filename, mimeType)
                val effectiveMime = if (mimeType.isNullOrBlank() || mimeType == "application/octet-stream") {
                    guessMimeType(cleanFilename)
                } else {
                    mimeType
                }

                var savedUri: Uri? = null
                var savedFilePath = ""
                var bytesCopied = 0L

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, cleanFilename)
                        put(MediaStore.Downloads.MIME_TYPE, effectiveMime)
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw Exception("Failed to insert MediaStore record")

                    resolver.openOutputStream(uri)?.use { os ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            os.write(buffer, 0, read)
                            bytesCopied += read
                        }
                        os.flush()
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    savedUri = uri
                    savedFilePath = "Downloads/$FOLDER_NAME/$cleanFilename"
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val targetDir = File(downloadsDir, FOLDER_NAME)
                    if (!targetDir.exists()) {
                        targetDir.mkdirs()
                    }
                    val targetFile = getUniqueFile(targetDir, cleanFilename)
                    targetFile.outputStream().use { os ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            os.write(buffer, 0, read)
                            bytesCopied += read
                        }
                        os.flush()
                    }
                    savedFilePath = targetFile.absolutePath
                    savedUri = Uri.fromFile(targetFile)
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(effectiveMime),
                        null
                    )
                }

                val formattedSize = formatFileSize(bytesCopied)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Saved $cleanFilename ($formattedSize) to Downloads/$FOLDER_NAME",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete(true, savedFilePath, savedUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(false, e.message ?: "Unknown error", null)
                }
            }
        }
    }

    /**
     * Saves raw byte array to Downloads/ToolCraft/ directory.
     */
    fun saveBytes(
        context: Context,
        bytes: ByteArray,
        filename: String,
        mimeType: String?,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        saveStream(context, ByteArrayInputStream(bytes), filename, mimeType, bytes.size.toLong(), onComplete)
    }

    /**
     * Handles data: URI saving (both base64 and url-encoded text)
     */
    fun saveDataUri(
        context: Context,
        dataUri: String,
        suggestedFilename: String? = null,
        fallbackMimeType: String? = null,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        try {
            val commaIndex = dataUri.indexOf(',')
            if (commaIndex == -1) {
                Toast.makeText(context, "Invalid data URI", Toast.LENGTH_SHORT).show()
                return
            }

            val header = dataUri.substring(0, commaIndex)
            val dataPart = dataUri.substring(commaIndex + 1)

            val mimeType = if (header.contains(":")) {
                val semicolon = header.indexOf(';')
                if (semicolon != -1) {
                    header.substring(header.indexOf(':') + 1, semicolon)
                } else {
                    header.substring(header.indexOf(':') + 1)
                }
            } else {
                fallbackMimeType ?: "application/octet-stream"
            }

            val isBase64 = header.contains(";base64", ignoreCase = true)
            val filename = suggestedFilename?.takeIf { it.isNotBlank() }
                ?: ("download_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + extensionForMime(mimeType))

            if (isBase64) {
                // If small data uri, decode directly; otherwise stream chunks
                val bytes = Base64.decode(dataPart, Base64.DEFAULT)
                saveBytes(context, bytes, filename, mimeType, onComplete)
            } else {
                val text = URLDecoder.decode(dataPart, "UTF-8")
                saveText(context, text, filename, mimeType, onComplete)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to parse data URL: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles Base64 string directly from JavaScript interface
     */
    fun saveBase64Data(
        context: Context,
        dataUriOrBase64: String,
        suggestedFilename: String,
        mimeType: String,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        if (dataUriOrBase64.startsWith("data:")) {
            saveDataUri(context, dataUriOrBase64, suggestedFilename, mimeType, onComplete)
        } else {
            try {
                val bytes = Base64.decode(dataUriOrBase64, Base64.DEFAULT)
                saveBytes(context, bytes, suggestedFilename, mimeType, onComplete)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Invalid base64 payload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles raw text download (e.g. JSON, TXT, CSV, HTML, MD)
     */
    fun saveText(
        context: Context,
        content: String,
        filename: String,
        mimeType: String = "text/plain",
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        saveBytes(context, bytes, filename, mimeType, onComplete)
    }

    /**
     * Downloads an HTTP/HTTPS URL into Downloads/ToolCraft/ via direct network streaming.
     */
    fun downloadHttp(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        filename: String?,
        onComplete: (Boolean, String, Uri?) -> Unit = { _, _, _ -> }
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resolvedFilename = sanitizeFilename(
                    filename?.takeIf { it.isNotBlank() }
                        ?: URLUtil.guessFileName(url, contentDisposition, mimeType),
                    mimeType
                )

                // Try DownloadManager first for background system download
                try {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    if (dm != null) {
                        val request = DownloadManager.Request(Uri.parse(url)).apply {
                            setMimeType(mimeType ?: guessMimeType(resolvedFilename))
                            userAgent?.let { addRequestHeader("User-Agent", it) }
                            setDescription("Downloading to Downloads/$FOLDER_NAME")
                            setTitle(resolvedFilename)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "$FOLDER_NAME/$resolvedFilename"
                            )
                        }
                        dm.enqueue(request)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Downloading $resolvedFilename to Downloads/$FOLDER_NAME",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }
                } catch (_: Exception) {
                    // Fallback to streaming download below
                }

                // Streaming connection: streams directly from socket into file without buffering entire file in RAM
                val conn = URL(url).openConnection() as HttpURLConnection
                userAgent?.let { conn.setRequestProperty("User-Agent", it) }
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.connect()

                val contentLength = conn.contentLengthLong
                conn.inputStream.use { inStream ->
                    saveStream(context, inStream, resolvedFilename, mimeType, contentLength, onComplete)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sanitizeFilename(name: String, mimeType: String?): String {
        var clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        if (clean.isBlank()) {
            clean = "tool_file_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        }
        if (!clean.contains(".")) {
            clean += extensionForMime(mimeType)
        }
        return clean
    }

    private fun guessMimeType(filename: String): String {
        val ext = filename.substringAfterLast(".", "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun extensionForMime(mimeType: String?): String {
        if (mimeType.isNullOrBlank()) return ".bin"
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return when {
            ext != null -> ".$ext"
            mimeType.contains("json") -> ".json"
            mimeType.contains("html") -> ".html"
            mimeType.contains("csv") -> ".csv"
            mimeType.contains("markdown") || mimeType.contains("text/md") -> ".md"
            mimeType.contains("png") -> ".png"
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
            mimeType.contains("webp") -> ".webp"
            mimeType.contains("svg") -> ".svg"
            mimeType.contains("pdf") -> ".pdf"
            mimeType.contains("zip") -> ".zip"
            mimeType.contains("text") -> ".txt"
            mimeType.contains("mp4") -> ".mp4"
            mimeType.contains("mp3") -> ".mp3"
            else -> ".bin"
        }
    }

    private fun getUniqueFile(dir: File, filename: String): File {
        var candidate = File(dir, filename)
        if (!candidate.exists()) return candidate

        val baseName = filename.substringBeforeLast(".")
        val extension = if (filename.contains(".")) "." + filename.substringAfterLast(".") else ""
        var count = 1
        while (candidate.exists()) {
            candidate = File(dir, "$baseName ($count)$extension")
            count++
        }
        return candidate
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
