/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.ToolDao
import com.example.data.model.Tool
import com.example.engine.SampleTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ToolRepository(
    private val toolDao: ToolDao,
    private val context: Context
) {

    private val sourcesDir = File(context.filesDir, "tool_sources")

    val allTools: Flow<List<Tool>> = toolDao.getAllTools()
    val recentTools: Flow<List<Tool>> = toolDao.getRecentTools()
    val distinctGroups: Flow<List<String>> = toolDao.getDistinctGroups()

    fun getToolById(id: Long): Flow<Tool?> = toolDao.getToolById(id).map { tool ->
        tool?.copy(htmlCode = readSourceCode(tool.htmlCode))
    }

    suspend fun getToolByIdDirect(id: Long): Tool? = withContext(Dispatchers.IO) {
        val tool = toolDao.getToolByIdDirect(id) ?: return@withContext null
        tool.copy(htmlCode = readSourceCode(tool.htmlCode))
    }

    suspend fun insertTool(tool: Tool): Long = withContext(Dispatchers.IO) {
        val originalCode = tool.htmlCode
        // Large source threshold (200 KB) to prevent SQLite CursorWindow 2MB limitation
        if (originalCode.length > 200_000) {
            val placeholderTool = tool.copy(htmlCode = "__PENDING_LARGE_FILE__")
            val id = toolDao.insertTool(placeholderTool)
            val marker = saveLargeSourceToFile(id, originalCode)
            toolDao.updateTool(placeholderTool.copy(id = id, htmlCode = marker))
            id
        } else {
            toolDao.insertTool(tool)
        }
    }

    suspend fun updateTool(tool: Tool) = withContext(Dispatchers.IO) {
        val originalCode = tool.htmlCode
        if (originalCode.length > 200_000) {
            val marker = saveLargeSourceToFile(tool.id, originalCode)
            toolDao.updateTool(tool.copy(htmlCode = marker))
        } else {
            // Clean up any old external source file if replaced with smaller code
            deleteSourceFile(tool.id)
            toolDao.updateTool(tool)
        }
    }

    suspend fun deleteTool(tool: Tool) = withContext(Dispatchers.IO) {
        deleteSourceFile(tool.id)
        tool.projectDir?.let { pDir ->
            try {
                val folder = com.example.engine.ZipToolManager.getToolProjectDir(context, pDir)
                if (folder.exists()) folder.deleteRecursively()
            } catch (_: Exception) {}
        }
        tool.thumbnailPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        toolDao.deleteTool(tool)
    }

    suspend fun deleteToolById(id: Long) = withContext(Dispatchers.IO) {
        val tool = toolDao.getToolByIdDirect(id)
        if (tool != null) {
            deleteTool(tool)
        } else {
            deleteSourceFile(id)
            toolDao.deleteToolById(id)
        }
    }

    suspend fun recordToolAccess(id: Long) = withContext(Dispatchers.IO) {
        toolDao.updateLastAccessed(id, System.currentTimeMillis())
    }

    suspend fun saveThumbnail(toolId: Long, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val thumbDir = File(context.filesDir, "thumbnails")
            if (!thumbDir.exists()) thumbDir.mkdirs()

            val thumbFile = File(thumbDir, "tool_${toolId}.png")
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            val path = thumbFile.absolutePath
            toolDao.updateThumbnail(toolId, path)
            path
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun reseedDefaultTools() = withContext(Dispatchers.IO) {
        toolDao.deleteBuiltInTools()
        val defaults = SampleTools.getDefaultTools(context)
        if (defaults.isNotEmpty()) {
            toolDao.insertTools(defaults)
        }
    }

    suspend fun syncDefaultToolsIfNeeded() = withContext(Dispatchers.IO) {
        val builtInCount = toolDao.getBuiltInCount()
        val defaults = SampleTools.getDefaultTools(context)
        val defaultNames = defaults.map { it.name }.toSet()
        val existingBuiltIns = toolDao.getAllToolsDirect().filter { it.isBuiltIn }
        val namesMatch = existingBuiltIns.map { it.name }.toSet() == defaultNames

        // If built-in tools don't match latest defaults, resync
        if (builtInCount != defaults.size || !namesMatch) {
            toolDao.deleteBuiltInTools()
            if (defaults.isNotEmpty()) {
                toolDao.insertTools(defaults)
            }
        } else {
            // Keep existing built-ins updated with latest unpacked assets
            existingBuiltIns.forEach { existing ->
                defaults.find { it.name.equals(existing.name, ignoreCase = true) }?.let { latest ->
                    val updated = existing.copy(
                        htmlCode = latest.htmlCode,
                        description = latest.description,
                        group = latest.group,
                        projectType = latest.projectType,
                        projectDir = latest.projectDir,
                        entryPoint = latest.entryPoint
                    )
                    toolDao.updateTool(updated)
                }
            }
        }
    }

    suspend fun exportToolsToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "WebTool Runner")

        val array = JSONArray()
        val all = toolDao.getAllToolsDirect()
        all.forEach { tool ->
            val obj = JSONObject().apply {
                put("id", tool.id)
                put("name", tool.name)
                put("group", tool.group)
                put("htmlCode", readSourceCode(tool.htmlCode))
                put("isBuiltIn", tool.isBuiltIn)
                put("createdAt", tool.createdAt)
            }
            array.put(obj)
        }
        root.put("tools", array)
        root.toString(2)
    }

    suspend fun importToolsFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("tools") ?: JSONArray()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val code = obj.optString("htmlCode", "<h1>Imported</h1>")
                val tool = Tool(
                    id = 0,
                    name = obj.optString("name", "Imported Tool"),
                    group = obj.optString("group", "Imported"),
                    htmlCode = code,
                    isBuiltIn = false,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    lastAccessedAt = System.currentTimeMillis()
                )
                insertTool(tool)
                count++
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        count
    }

    private fun saveLargeSourceToFile(toolId: Long, code: String): String {
        try {
            if (!sourcesDir.exists()) sourcesDir.mkdirs()
            val sourceFile = File(sourcesDir, "tool_${toolId}_source.html")
            sourceFile.writeText(code, Charsets.UTF_8)
            return "__FILE_SOURCE__:tool_${toolId}_source.html"
        } catch (e: Exception) {
            e.printStackTrace()
            return code
        }
    }

    private fun readSourceCode(htmlCode: String): String {
        if (htmlCode.startsWith("__FILE_SOURCE__:")) {
            val fileName = htmlCode.substringAfter("__FILE_SOURCE__:")
            val file = File(sourcesDir, fileName)
            if (file.exists()) {
                return try {
                    file.readText(Charsets.UTF_8)
                } catch (e: Exception) {
                    htmlCode
                }
            }
        }
        return htmlCode
    }

    suspend fun importZipTool(inputStream: java.io.InputStream, suggestedName: String?): Long = withContext(Dispatchers.IO) {
        val extracted = com.example.engine.ZipToolManager.extractZipStream(context, inputStream, suggestedName)
        val tool = Tool(
            id = 0,
            name = extracted.toolName,
            group = extracted.group,
            description = extracted.description,
            htmlCode = extracted.entryHtmlCode,
            projectType = Tool.PROJECT_TYPE_MULTI,
            projectDir = extracted.projectDirName,
            entryPoint = extracted.entryPoint,
            isBuiltIn = false,
            createdAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )
        insertTool(tool)
    }

    suspend fun importHtmlTool(inputStream: java.io.InputStream, suggestedName: String?): Long = withContext(Dispatchers.IO) {
        val extracted = com.example.engine.ZipToolManager.extractSingleHtml(inputStream, suggestedName)
        val tool = Tool(
            id = 0,
            name = extracted.toolName,
            group = extracted.group,
            htmlCode = extracted.htmlCode,
            projectType = Tool.PROJECT_TYPE_SINGLE,
            isBuiltIn = false,
            createdAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )
        insertTool(tool)
    }

    suspend fun exportToolAsZip(toolId: Long, outputStream: java.io.OutputStream): Boolean = withContext(Dispatchers.IO) {
        val tool = getToolByIdDirect(toolId) ?: return@withContext false
        try {
            if (tool.isMultiFile && tool.projectDir != null) {
                val dir = com.example.engine.ZipToolManager.getToolProjectDir(context, tool.projectDir)
                if (dir.exists()) {
                    com.example.engine.ZipToolManager.compressProjectToZipStream(dir, outputStream)
                    return@withContext true
                }
            }
            // Fallback or single file export
            com.example.engine.ZipToolManager.compressSingleHtmlToZipStream(tool, outputStream)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getProjectDir(tool: Tool): File? {
        val dirName = tool.projectDir ?: return null
        val dir = com.example.engine.ZipToolManager.getToolProjectDir(context, dirName)
        return if (dir.exists()) dir else null
    }

    private fun deleteSourceFile(toolId: Long) {
        try {
            val sourceFile = File(sourcesDir, "tool_${toolId}_source.html")
            if (sourceFile.exists()) sourceFile.delete()
        } catch (_: Exception) {}
    }
}
