/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.engine

import android.content.Context
import com.example.data.model.Tool
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProjectFileInfo(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val extension: String
)

data class ExtractedProjectResult(
    val projectDirName: String,
    val toolName: String,
    val group: String,
    val description: String,
    val entryPoint: String,
    val entryHtmlCode: String
)

data class ExtractedHtmlResult(
    val toolName: String,
    val group: String,
    val htmlCode: String
)

object ZipToolManager {

    fun getProjectsBaseDir(context: Context): File {
        val dir = File(context.filesDir, "tool_projects")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getToolProjectDir(context: Context, projectDirName: String): File {
        return File(getProjectsBaseDir(context), projectDirName)
    }

    /**
     * Extracts a ZIP stream securely into a unique project folder.
     * Prevents Zip Slip attacks, unwraps single root folders, auto-detects entryPoint,
     * and parses tool.json or manifest.json if present.
     */
    fun extractZipStream(
        context: Context,
        inputStream: InputStream,
        suggestedName: String? = null
    ): ExtractedProjectResult {
        val baseDir = getProjectsBaseDir(context)
        val dirName = "proj_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val projectDir = File(baseDir, dirName)
        if (!projectDir.exists()) projectDir.mkdirs()

        val tempExtractDir = File(baseDir, "temp_${dirName}")
        if (!tempExtractDir.exists()) tempExtractDir.mkdirs()

        try {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val rawName = entry.name
                    val name = rawName.replace('\\', '/')
                    // Ignore macOS metadata and dangerous entries
                    if (!name.startsWith("__MACOSX") && !name.contains("/.DS_Store") && !name.endsWith(".DS_Store")) {
                        val destinationFile = File(tempExtractDir, name)
                        // Zip Slip validation
                        val canonicalDest = destinationFile.canonicalPath
                        val canonicalDir = tempExtractDir.canonicalPath
                        if (!canonicalDest.startsWith(canonicalDir + File.separator) && canonicalDest != canonicalDir) {
                            throw SecurityException("Zip entry attempted directory traversal: $name")
                        }

                        if (entry.isDirectory) {
                            destinationFile.mkdirs()
                        } else {
                            destinationFile.parentFile?.mkdirs()
                            FileOutputStream(destinationFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // Directory Hoisting: If all extracted files are under a single root folder, unwrap it
            val tempChildren = tempExtractDir.listFiles()?.filter { !it.name.startsWith(".") } ?: emptyList()
            val effectiveSourceDir = if (tempChildren.size == 1 && tempChildren.first().isDirectory) {
                tempChildren.first()
            } else {
                tempExtractDir
            }

            // Copy contents from effectiveSourceDir to projectDir
            copyDirectoryContents(effectiveSourceDir, projectDir)

        } finally {
            // Clean up temporary folder
            tempExtractDir.deleteRecursively()
        }

        // Auto-detect entryPoint
        var entryPoint = "index.html"
        val allProjectFiles = listProjectFiles(projectDir)
        val htmlFiles = allProjectFiles.filter { !it.isDirectory && (it.extension == "html" || it.extension == "htm") }

        when {
            File(projectDir, "index.html").exists() -> entryPoint = "index.html"
            File(projectDir, "index.htm").exists() -> entryPoint = "index.htm"
            File(projectDir, "main.html").exists() -> entryPoint = "main.html"
            File(projectDir, "app.html").exists() -> entryPoint = "app.html"
            htmlFiles.isNotEmpty() -> entryPoint = htmlFiles.first().relativePath
            else -> {
                // Fallback: create default index.html if none found
                val fallbackFile = File(projectDir, "index.html")
                fallbackFile.writeText("<!DOCTYPE html><html><head><title>Project</title></head><body><h1>Project Initialized</h1></body></html>")
                entryPoint = "index.html"
            }
        }

        // Check for metadata file (tool.json or manifest.json)
        var parsedName: String? = null
        var parsedGroup = "Imported"
        var parsedDescription = ""

        val manifestFile = when {
            File(projectDir, "tool.json").exists() -> File(projectDir, "tool.json")
            File(projectDir, "manifest.json").exists() -> File(projectDir, "manifest.json")
            else -> null
        }

        if (manifestFile != null) {
            try {
                val json = JSONObject(manifestFile.readText(Charsets.UTF_8))
                if (json.has("name") && json.getString("name").isNotBlank()) {
                    parsedName = json.getString("name")
                }
                if (json.has("category")) {
                    parsedGroup = json.getString("category")
                } else if (json.has("group")) {
                    parsedGroup = json.getString("group")
                }
                if (json.has("description")) {
                    parsedDescription = json.getString("description")
                }
                if (json.has("entryPoint") && File(projectDir, json.getString("entryPoint")).exists()) {
                    entryPoint = json.getString("entryPoint")
                }
            } catch (_: Exception) {}
        }

        // Read entry file text
        val entryFile = File(projectDir, entryPoint)
        val entryHtmlContent = if (entryFile.exists()) entryFile.readText(Charsets.UTF_8) else ""

        // If name wasn't in tool.json, try to extract from <title> tag in the HTML entry point
        if (parsedName == null) {
            val titleRegex = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)
            val match = titleRegex.find(entryHtmlContent)
            if (match != null && match.groupValues[1].isNotBlank()) {
                parsedName = match.groupValues[1].trim()
            }
        }

        // If still null, format from suggestedName (e.g. filename)
        if (parsedName == null && !suggestedName.isNullOrBlank()) {
            parsedName = suggestedName.substringBeforeLast(".")
                .replace(Regex("[-_]"), " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        val finalName = parsedName ?: "Imported Web App"

        return ExtractedProjectResult(
            projectDirName = dirName,
            toolName = finalName,
            group = parsedGroup,
            description = parsedDescription,
            entryPoint = entryPoint,
            entryHtmlCode = entryHtmlContent
        )
    }

    /**
     * Extracts a single HTML file from input stream and parses its title.
     */
    fun extractSingleHtml(
        inputStream: InputStream,
        originalFileName: String? = null
    ): ExtractedHtmlResult {
        val content = inputStream.bufferedReader(Charsets.UTF_8).readText()
        var name: String? = null
        val titleMatch = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE).find(content)
        if (titleMatch != null && titleMatch.groupValues[1].isNotBlank()) {
            name = titleMatch.groupValues[1].trim()
        }
        if (name == null && !originalFileName.isNullOrBlank()) {
            name = originalFileName.substringBeforeLast(".")
                .replace(Regex("[-_]"), " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
        val finalName = name ?: "Imported Tool"
        return ExtractedHtmlResult(
            toolName = finalName,
            group = "Imported",
            htmlCode = content
        )
    }

    /**
     * Lists all files and folders in a project directory, sorted folders first, then alphabetical.
     */
    fun listProjectFiles(projectDir: File): List<ProjectFileInfo> {
        if (!projectDir.exists() || !projectDir.isDirectory) return emptyList()
        val result = mutableListOf<ProjectFileInfo>()
        val baseCanonical = projectDir.canonicalPath

        fun scan(dir: File) {
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (file.name.startsWith(".") || file.name == "__MACOSX") continue
                val relPath = file.canonicalPath.removePrefix(baseCanonical).removePrefix(File.separator)
                val ext = file.extension.lowercase()
                result.add(
                    ProjectFileInfo(
                        relativePath = relPath,
                        name = file.name,
                        isDirectory = file.isDirectory,
                        size = if (file.isDirectory) 0L else file.length(),
                        extension = ext
                    )
                )
                if (file.isDirectory) {
                    scan(file)
                }
            }
        }

        scan(projectDir)
        return result.sortedWith(
            compareBy<ProjectFileInfo> { !it.isDirectory }
                .thenBy { it.relativePath.lowercase() }
        )
    }

    fun readFileContent(projectDir: File, relativePath: String): String {
        val file = File(projectDir, relativePath)
        if (!file.canonicalPath.startsWith(projectDir.canonicalPath)) {
            throw SecurityException("Directory traversal attempt")
        }
        return if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else ""
    }

    fun writeFileContent(projectDir: File, relativePath: String, content: String): Boolean {
        return try {
            val file = File(projectDir, relativePath)
            if (!file.canonicalPath.startsWith(projectDir.canonicalPath)) return false
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFile(projectDir: File, relativePath: String, initialContent: String = ""): Boolean {
        return try {
            val cleanPath = relativePath.trim().removePrefix("/")
            val file = File(projectDir, cleanPath)
            if (!file.canonicalPath.startsWith(projectDir.canonicalPath)) return false
            if (file.exists()) return false
            file.parentFile?.mkdirs()
            file.writeText(initialContent, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createNewFolder(projectDir: File, relativePath: String): Boolean {
        return try {
            val cleanPath = relativePath.trim().removePrefix("/")
            val dir = File(projectDir, cleanPath)
            if (!dir.canonicalPath.startsWith(projectDir.canonicalPath)) return false
            if (dir.exists()) return true
            dir.mkdirs()
        } catch (_: Exception) {
            false
        }
    }

    fun deleteFileOrFolder(projectDir: File, relativePath: String): Boolean {
        return try {
            val file = File(projectDir, relativePath)
            if (!file.canonicalPath.startsWith(projectDir.canonicalPath)) return false
            if (!file.exists()) return true
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (_: Exception) {
            false
        }
    }

    fun renameFileOrFolder(projectDir: File, oldRelativePath: String, newRelativePath: String): Boolean {
        return try {
            val oldFile = File(projectDir, oldRelativePath)
            val newFile = File(projectDir, newRelativePath)
            if (!oldFile.canonicalPath.startsWith(projectDir.canonicalPath) ||
                !newFile.canonicalPath.startsWith(projectDir.canonicalPath)) return false
            if (!oldFile.exists() || newFile.exists()) return false
            newFile.parentFile?.mkdirs()
            oldFile.renameTo(newFile)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Converts a single-file HTML Tool into a full multi-file project folder.
     */
    fun convertSingleFileToProject(context: Context, tool: Tool): String {
        val baseDir = getProjectsBaseDir(context)
        val dirName = "proj_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val projectDir = File(baseDir, dirName)
        projectDir.mkdirs()

        // 1. Write index.html
        File(projectDir, "index.html").writeText(tool.htmlCode, Charsets.UTF_8)

        // 2. Write tool.json manifest
        val manifest = JSONObject().apply {
            put("name", tool.name)
            put("group", tool.group)
            put("entryPoint", "index.html")
            put("version", "1.0.0")
            put("convertedFromSingleFile", true)
        }
        File(projectDir, "tool.json").writeText(manifest.toString(2), Charsets.UTF_8)

        return dirName
    }

    /**
     * Compresses the entire multi-file project directory into a ZIP output stream.
     */
    fun compressProjectToZipStream(projectDir: File, outputStream: OutputStream) {
        val baseCanonical = projectDir.canonicalPath
        ZipOutputStream(outputStream).use { zos ->
            fun addDir(dir: File) {
                val files = dir.listFiles() ?: return
                for (file in files) {
                    if (file.name.startsWith(".") || file.name == "__MACOSX") continue
                    val relPath = file.canonicalPath.removePrefix(baseCanonical).removePrefix(File.separator)
                    if (file.isDirectory) {
                        val entry = ZipEntry("$relPath/")
                        zos.putNextEntry(entry)
                        zos.closeEntry()
                        addDir(file)
                    } else {
                        val entry = ZipEntry(relPath)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
            addDir(projectDir)
            zos.finish()
        }
    }

    /**
     * Compresses a single HTML tool into a downloadable ZIP containing index.html and tool.json.
     */
    fun compressSingleHtmlToZipStream(tool: Tool, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // Add index.html
            val htmlEntry = ZipEntry("index.html")
            zos.putNextEntry(htmlEntry)
            zos.write(tool.htmlCode.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Add tool.json
            val manifest = JSONObject().apply {
                put("name", tool.name)
                put("group", tool.group)
                put("entryPoint", "index.html")
                put("version", "1.0.0")
            }
            val manifestEntry = ZipEntry("tool.json")
            zos.putNextEntry(manifestEntry)
            zos.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.finish()
        }
    }

    private fun copyDirectoryContents(source: File, target: File) {
        val files = source.listFiles() ?: return
        for (file in files) {
            val dest = File(target, file.name)
            if (file.isDirectory) {
                dest.mkdirs()
                copyDirectoryContents(file, dest)
            } else {
                dest.parentFile?.mkdirs()
                file.copyTo(dest, overwrite = true)
            }
        }
    }
}
