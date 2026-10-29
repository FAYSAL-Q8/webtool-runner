/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.engine

/**
 * Default sample tool loader.
 * @author Faysal Alibrahim
 */

import android.content.Context
import com.example.data.model.Tool
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object SampleTools {

    val DEFAULT_ZIP_NAMES = listOf(
        "photo-scheduler.zip",
        "ciphervault.zip",
        "dev-toolbox.zip",
        "har-reader.zip"
    )

    fun getDefaultTools(context: Context? = null): List<Tool> {
        val tools = mutableListOf<Tool>()

        for (zipName in DEFAULT_ZIP_NAMES) {
            val toolSlug = zipName.removeSuffix(".zip")
            val targetDirName = "default_${toolSlug.replace('-', '_')}"
            val targetDir = if (context != null) {
                ZipToolManager.getToolProjectDir(context, targetDirName)
            } else {
                File(System.getProperty("java.io.tmpdir"), "tool_projects/$targetDirName")
            }

            try {
                val indexFile = File(targetDir, "index.html")
                val manifestFile = File(targetDir, "tool.json")

                val stream = openZipInputStream(context, zipName)
                if (stream != null) {
                    stream.use { input ->
                        targetDir.mkdirs()
                        extractZipToFolder(input, targetDir)
                    }
                }

                if (indexFile.exists()) {
                    val entryHtml = indexFile.readText(Charsets.UTF_8)
                    var toolName = toolSlug.replace('-', ' ')
                        .split(' ')
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    var group = "Utility"
                    var description = ""
                    var entryPoint = "index.html"

                    if (manifestFile.exists()) {
                        try {
                            val json = JSONObject(manifestFile.readText(Charsets.UTF_8))
                            if (json.has("name") && json.getString("name").isNotBlank()) {
                                toolName = json.getString("name")
                            }
                            if (json.has("category") && json.getString("category").isNotBlank()) {
                                group = json.getString("category")
                            } else if (json.has("group") && json.getString("group").isNotBlank()) {
                                group = json.getString("group")
                            }
                            if (json.has("description")) {
                                description = json.getString("description")
                            }
                            if (json.has("entryPoint") && json.getString("entryPoint").isNotBlank()) {
                                entryPoint = json.getString("entryPoint")
                            }
                        } catch (_: Exception) {}
                    }

                    tools.add(
                        Tool(
                            name = toolName,
                            group = group,
                            description = description,
                            htmlCode = entryHtml,
                            projectType = Tool.PROJECT_TYPE_MULTI,
                            projectDir = targetDirName,
                            entryPoint = entryPoint,
                            isBuiltIn = true
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return tools
    }

    private fun openZipInputStream(context: Context?, zipName: String): InputStream? {
        if (context != null) {
            try {
                return context.assets.open("default_tools/$zipName")
            } catch (_: Exception) {}
        }

        val candidates = listOf(
            File("app/src/main/assets/default_tools/$zipName"),
            File("/app/applet/app/src/main/assets/default_tools/$zipName")
        )
        for (f in candidates) {
            if (f.exists() && f.isFile && f.length() > 0) {
                return try {
                    FileInputStream(f)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun extractZipToFolder(inputStream: InputStream, targetDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val rawName = entry.name
                val name = rawName.replace('\\', '/')
                if (!name.startsWith("__MACOSX") && !name.contains("/.DS_Store") && !name.endsWith(".DS_Store")) {
                    val destFile = File(targetDir, name)
                    val canonicalDest = destFile.canonicalPath
                    val canonicalDir = targetDir.canonicalPath
                    if (!canonicalDest.startsWith(canonicalDir + File.separator) && canonicalDest != canonicalDir) {
                        throw SecurityException("Directory traversal in zip entry: $name")
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
