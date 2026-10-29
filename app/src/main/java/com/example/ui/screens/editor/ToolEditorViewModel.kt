/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Tool
import com.example.data.repository.ToolRepository
import com.example.engine.ProjectFileInfo
import com.example.engine.ZipToolManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

enum class EditorTab {
    INFO,
    CODE,
    PREVIEW
}

data class EditorUiState(
    val toolId: Long? = null,
    val name: String = "",
    val group: String = "Utility",
    val description: String = "",
    val htmlCode: String = "",
    val projectType: String = Tool.PROJECT_TYPE_SINGLE,
    val projectDir: String? = null,
    val entryPoint: String = "index.html",
    val projectFiles: List<ProjectFileInfo> = emptyList(),
    val selectedFileRelativePath: String = "index.html",
    val selectedFileContent: String = "",
    val isFileExplorerOpen: Boolean = false,
    val currentTab: EditorTab = EditorTab.INFO,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val existingGroups: List<String> = emptyList()
) {
    val isMultiFile: Boolean get() = projectType == Tool.PROJECT_TYPE_MULTI
}

class ToolEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ToolRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ToolRepository(db.toolDao(), application)

        viewModelScope.launch {
            repository.distinctGroups.collect { groups ->
                _uiState.value = _uiState.value.copy(existingGroups = groups)
            }
        }
    }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadTool(id: Long?) {
        if (id == null || id <= 0L) {
            // New Tool
            val blankCode = getTemplate("BLANK")
            _uiState.value = EditorUiState(
                toolId = null,
                name = "New Tool",
                group = "Utility",
                description = "",
                htmlCode = blankCode,
                projectType = Tool.PROJECT_TYPE_SINGLE,
                projectDir = null,
                entryPoint = "index.html",
                selectedFileRelativePath = "index.html",
                selectedFileContent = blankCode,
                currentTab = EditorTab.INFO
            )
            return
        }

        viewModelScope.launch {
            val tool = repository.getToolByIdDirect(id)
            if (tool != null) {
                var files = emptyList<ProjectFileInfo>()
                var activeFile = tool.entryPoint
                var fileContent = tool.htmlCode

                if (tool.isMultiFile && tool.projectDir != null) {
                    val pDir = ZipToolManager.getToolProjectDir(getApplication(), tool.projectDir)
                    if (pDir.exists()) {
                        files = ZipToolManager.listProjectFiles(pDir)
                        val entryFile = File(pDir, tool.entryPoint)
                        if (entryFile.exists()) {
                            activeFile = tool.entryPoint
                            fileContent = entryFile.readText(Charsets.UTF_8)
                        } else if (files.isNotEmpty()) {
                            val firstText = files.firstOrNull { !it.isDirectory }
                            if (firstText != null) {
                                activeFile = firstText.relativePath
                                fileContent = ZipToolManager.readFileContent(pDir, activeFile)
                            }
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    toolId = tool.id,
                    name = tool.name,
                    group = tool.group,
                    description = tool.description,
                    htmlCode = tool.htmlCode,
                    projectType = tool.projectType,
                    projectDir = tool.projectDir,
                    entryPoint = tool.entryPoint,
                    projectFiles = files,
                    selectedFileRelativePath = activeFile,
                    selectedFileContent = fileContent,
                    currentTab = EditorTab.INFO
                )
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.value = _uiState.value.copy(name = newName)
    }

    fun onGroupChanged(newGroup: String) {
        _uiState.value = _uiState.value.copy(group = newGroup)
    }

    fun onDescriptionChanged(newDesc: String) {
        _uiState.value = _uiState.value.copy(description = newDesc)
    }

    fun onHtmlCodeChanged(newCode: String) {
        val current = _uiState.value
        if (current.isMultiFile && current.projectDir != null) {
            val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
            ZipToolManager.writeFileContent(pDir, current.selectedFileRelativePath, newCode)
            val updatedHtmlCode = if (current.selectedFileRelativePath == current.entryPoint) newCode else current.htmlCode
            _uiState.value = current.copy(
                selectedFileContent = newCode,
                htmlCode = updatedHtmlCode
            )
        } else {
            _uiState.value = current.copy(
                htmlCode = newCode,
                selectedFileContent = newCode
            )
        }
    }

    fun onTabChanged(tab: EditorTab) {
        // Save current code to disk if multi-file before switching tabs
        saveCurrentActiveFileToDisk()
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun toggleFileExplorer() {
        _uiState.value = _uiState.value.copy(isFileExplorerOpen = !_uiState.value.isFileExplorerOpen)
    }

    fun setFileExplorerOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isFileExplorerOpen = isOpen)
    }

    fun selectFile(relativePath: String) {
        val current = _uiState.value
        if (!current.isMultiFile || current.projectDir == null) return

        // Save current file first
        saveCurrentActiveFileToDisk()

        val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
        val content = ZipToolManager.readFileContent(pDir, relativePath)

        _uiState.value = current.copy(
            selectedFileRelativePath = relativePath,
            selectedFileContent = content
        )
    }

    fun refreshProjectFiles() {
        val current = _uiState.value
        if (current.isMultiFile && current.projectDir != null) {
            val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
            val files = ZipToolManager.listProjectFiles(pDir)
            _uiState.value = current.copy(projectFiles = files)
        }
    }

    fun createNewFile(relativePath: String) {
        val current = _uiState.value
        ensureMultiFileProject { pDir ->
            val clean = relativePath.trim().removePrefix("/")
            if (clean.isBlank()) return@ensureMultiFileProject
            val initial = if (clean.endsWith(".html") || clean.endsWith(".htm")) {
                "<!DOCTYPE html>\n<html>\n<head>\n  <meta charset=\"UTF-8\" />\n  <title>${clean.substringBeforeLast(".")}</title>\n</head>\n<body>\n  <h1>${clean}</h1>\n</body>\n</html>"
            } else if (clean.endsWith(".css")) {
                "/* ${clean} */\nbody {\n  margin: 0;\n  padding: 16px;\n}\n"
            } else if (clean.endsWith(".js")) {
                "// ${clean}\nconsole.log('${clean} loaded');\n"
            } else {
                ""
            }

            val created = ZipToolManager.createNewFile(pDir, clean, initial)
            if (created) {
                val files = ZipToolManager.listProjectFiles(pDir)
                _uiState.value = _uiState.value.copy(
                    projectFiles = files,
                    selectedFileRelativePath = clean,
                    selectedFileContent = initial,
                    statusMessage = "Created $clean"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "File already exists or invalid path")
            }
        }
    }

    fun createNewFolder(relativePath: String) {
        val current = _uiState.value
        ensureMultiFileProject { pDir ->
            val clean = relativePath.trim().removePrefix("/")
            if (clean.isBlank()) return@ensureMultiFileProject
            val created = ZipToolManager.createNewFolder(pDir, clean)
            if (created) {
                val files = ZipToolManager.listProjectFiles(pDir)
                _uiState.value = _uiState.value.copy(
                    projectFiles = files,
                    statusMessage = "Created folder $clean"
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create folder")
            }
        }
    }

    fun deleteFile(relativePath: String) {
        val current = _uiState.value
        if (!current.isMultiFile || current.projectDir == null) return
        val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)

        val deleted = ZipToolManager.deleteFileOrFolder(pDir, relativePath)
        if (deleted) {
            val files = ZipToolManager.listProjectFiles(pDir)
            val nextActive = if (current.selectedFileRelativePath == relativePath) {
                current.entryPoint.takeIf { File(pDir, it).exists() }
                    ?: files.firstOrNull { !it.isDirectory }?.relativePath
                    ?: "index.html"
            } else {
                current.selectedFileRelativePath
            }
            val content = ZipToolManager.readFileContent(pDir, nextActive)
            _uiState.value = current.copy(
                projectFiles = files,
                selectedFileRelativePath = nextActive,
                selectedFileContent = content,
                statusMessage = "Deleted $relativePath"
            )
        }
    }

    fun renameFile(oldPath: String, newPath: String) {
        val current = _uiState.value
        if (!current.isMultiFile || current.projectDir == null) return
        val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)

        val renamed = ZipToolManager.renameFileOrFolder(pDir, oldPath, newPath)
        if (renamed) {
            val files = ZipToolManager.listProjectFiles(pDir)
            val updatedActive = if (current.selectedFileRelativePath == oldPath) newPath else current.selectedFileRelativePath
            val updatedEntryPoint = if (current.entryPoint == oldPath) newPath else current.entryPoint
            _uiState.value = current.copy(
                projectFiles = files,
                selectedFileRelativePath = updatedActive,
                entryPoint = updatedEntryPoint,
                statusMessage = "Renamed to $newPath"
            )
        } else {
            _uiState.value = current.copy(errorMessage = "Could not rename file")
        }
    }

    fun setAsEntryPoint(relativePath: String) {
        val current = _uiState.value
        if (!current.isMultiFile || current.projectDir == null) return
        val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
        val content = ZipToolManager.readFileContent(pDir, relativePath)
        _uiState.value = current.copy(
            entryPoint = relativePath,
            htmlCode = content,
            statusMessage = "Set $relativePath as Entry Point"
        )
    }

    fun convertToMultiFileProject() {
        val current = _uiState.value
        if (current.isMultiFile) return

        val tempTool = Tool(
            name = current.name,
            group = current.group,
            htmlCode = current.htmlCode
        )
        val dirName = ZipToolManager.convertSingleFileToProject(getApplication(), tempTool)
        val pDir = ZipToolManager.getToolProjectDir(getApplication(), dirName)
        val files = ZipToolManager.listProjectFiles(pDir)

        _uiState.value = current.copy(
            projectType = Tool.PROJECT_TYPE_MULTI,
            projectDir = dirName,
            entryPoint = "index.html",
            projectFiles = files,
            selectedFileRelativePath = "index.html",
            selectedFileContent = current.htmlCode,
            statusMessage = "Converted to Multi-File Project"
        )
    }

    fun importZipIntoProject(inputStream: InputStream, suggestedName: String?) {
        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.IO) {
                    ZipToolManager.extractZipStream(getApplication(), inputStream, suggestedName)
                }
                val pDir = ZipToolManager.getToolProjectDir(getApplication(), extracted.projectDirName)
                val files = ZipToolManager.listProjectFiles(pDir)

                _uiState.value = _uiState.value.copy(
                    name = if (_uiState.value.name.isBlank() || _uiState.value.name == "New Tool") extracted.toolName else _uiState.value.name,
                    group = if (_uiState.value.group == "Utility") extracted.group else _uiState.value.group,
                    description = if (_uiState.value.description.isBlank()) extracted.description else _uiState.value.description,
                    projectType = Tool.PROJECT_TYPE_MULTI,
                    projectDir = extracted.projectDirName,
                    entryPoint = extracted.entryPoint,
                    htmlCode = extracted.entryHtmlCode,
                    projectFiles = files,
                    selectedFileRelativePath = extracted.entryPoint,
                    selectedFileContent = extracted.entryHtmlCode,
                    currentTab = EditorTab.CODE,
                    statusMessage = "Imported ZIP project (${files.size} files)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "ZIP import failed: ${e.message}")
            }
        }
    }

    fun importHtmlIntoProject(inputStream: InputStream, filename: String?) {
        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.IO) {
                    ZipToolManager.extractSingleHtml(inputStream, filename)
                }
                val current = _uiState.value
                if (current.isMultiFile && current.projectDir != null) {
                    val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
                    val targetName = filename ?: "imported.html"
                    ZipToolManager.writeFileContent(pDir, targetName, extracted.htmlCode)
                    val files = ZipToolManager.listProjectFiles(pDir)
                    _uiState.value = current.copy(
                        projectFiles = files,
                        selectedFileRelativePath = targetName,
                        selectedFileContent = extracted.htmlCode,
                        currentTab = EditorTab.CODE,
                        statusMessage = "Imported $targetName"
                    )
                } else {
                    _uiState.value = current.copy(
                        name = if (current.name.isBlank() || current.name == "New Tool") extracted.toolName else current.name,
                        htmlCode = extracted.htmlCode,
                        selectedFileContent = extracted.htmlCode,
                        currentTab = EditorTab.CODE,
                        statusMessage = "Loaded ${filename ?: "HTML"}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "HTML import failed: ${e.message}")
            }
        }
    }

    fun exportProjectZip(outputStream: OutputStream): Boolean {
        saveCurrentActiveFileToDisk()
        val current = _uiState.value
        return try {
            if (current.isMultiFile && current.projectDir != null) {
                val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
                ZipToolManager.compressProjectToZipStream(pDir, outputStream)
            } else {
                val tool = Tool(name = current.name, group = current.group, htmlCode = current.htmlCode)
                ZipToolManager.compressSingleHtmlToZipStream(tool, outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun insertSnippet(snippet: String) {
        val current = _uiState.value
        val updated = current.selectedFileContent + "\n" + snippet
        onHtmlCodeChanged(updated)
    }

    fun applyTemplate(type: String) {
        val template = getTemplate(type)
        onHtmlCodeChanged(template)
    }

    fun saveTool(onSuccess: (Long) -> Unit) {
        val current = _uiState.value
        if (current.name.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Name cannot be empty")
            return
        }

        saveCurrentActiveFileToDisk()

        viewModelScope.launch {
            val id = current.toolId
            val finalId = if (id != null && id > 0) {
                val existing = repository.getToolByIdDirect(id)
                val updated = (existing ?: Tool(name = current.name, htmlCode = current.htmlCode)).copy(
                    name = current.name,
                    group = current.group,
                    description = current.description,
                    htmlCode = current.htmlCode,
                    projectType = current.projectType,
                    projectDir = current.projectDir,
                    entryPoint = current.entryPoint,
                    lastAccessedAt = System.currentTimeMillis()
                )
                repository.updateTool(updated)
                id
            } else {
                val newTool = Tool(
                    name = current.name,
                    group = current.group,
                    description = current.description,
                    htmlCode = current.htmlCode,
                    projectType = current.projectType,
                    projectDir = current.projectDir,
                    entryPoint = current.entryPoint,
                    lastAccessedAt = System.currentTimeMillis()
                )
                repository.insertTool(newTool)
            }

            _uiState.value = _uiState.value.copy(
                toolId = finalId,
                isSaved = true,
                statusMessage = "Tool saved successfully"
            )
            onSuccess(finalId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    private fun saveCurrentActiveFileToDisk() {
        val current = _uiState.value
        if (current.isMultiFile && current.projectDir != null) {
            val pDir = ZipToolManager.getToolProjectDir(getApplication(), current.projectDir)
            ZipToolManager.writeFileContent(pDir, current.selectedFileRelativePath, current.selectedFileContent)
        }
    }

    private inline fun ensureMultiFileProject(action: (File) -> Unit) {
        val current = _uiState.value
        if (!current.isMultiFile || current.projectDir == null) {
            convertToMultiFileProject()
        }
        val pDir = ZipToolManager.getToolProjectDir(getApplication(), _uiState.value.projectDir ?: return)
        action(pDir)
    }

    private fun getTemplate(type: String): String {
        return when (type) {
            "CANVAS" -> """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { margin:0; background:#0B0F19; overflow:hidden; display:flex; justify-content:center; align-items:center; height:100vh; font-family:sans-serif; }
  canvas { background:#111827; border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,0.5); }
</style>
</head>
<body>
<canvas id="c" width="320" height="320"></canvas>
<script>
  const c = document.getElementById('c');
  const ctx = c.getContext('2d');
  let t = 0;
  function loop() {
    ctx.fillStyle = 'rgba(11, 15, 25, 0.2)';
    ctx.fillRect(0,0,320,320);
    ctx.fillStyle = '#38BDF8';
    ctx.beginPath();
    ctx.arc(160 + Math.cos(t)*80, 160 + Math.sin(t)*80, 16, 0, Math.PI*2);
    ctx.fill();
    t += 0.05;
    requestAnimationFrame(loop);
  }
  loop();
</script>
</body>
</html>
""".trimIndent()
            "NATIVE_BRIDGE" -> """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Native Bridge Demo</title>
<style>
  body { font-family:sans-serif; background:#0F172A; color:#F8FAFC; padding:20px; display:flex; flex-direction:column; gap:12px; }
  button { padding:14px; background:#6366F1; color:#FFF; border:none; border-radius:10px; font-size:16px; font-weight:600; cursor:pointer; }
  button:active { background:#4F46E5; }
</style>
</head>
<body>
  <h2>Native Bridge Demo</h2>
  <button onclick="window.AndroidBridge.showToast('Hello from Web Tool!')">Show Toast</button>
  <button onclick="window.AndroidBridge.vibrate(50)">Haptic Vibrate</button>
  <button onclick="window.AndroidBridge.copyToClipboard('ToolCraft Bridge Secret')">Copy Secret</button>
  <button onclick="window.AndroidBridge.shareText('Web Tool', 'Check out this offline web app!')">Share Text</button>
</body>
</html>
""".trimIndent()
            else -> """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>New Tool</title>
  <style>
    body {
      margin: 0;
      padding: 24px;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: #090D16;
      color: #E2E8F0;
      min-height: 100vh;
      box-sizing: border-box;
    }
    .card {
      background: rgba(30, 41, 59, 0.7);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      padding: 24px;
      backdrop-filter: blur(12px);
    }
    h1 { margin-top: 0; color: #38BDF8; }
    p { line-height: 1.6; color: #94A3B8; }
    button {
      background: #6366F1;
      color: white;
      border: none;
      padding: 12px 20px;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
    }
  </style>
</head>
<body>
  <div class="card">
    <h1>Welcome to Your New Tool</h1>
    <p>This web tool runs offline with direct hardware acceleration and Android native bridge integration.</p>
    <button onclick="if(window.AndroidBridge) window.AndroidBridge.showToast('Ready to build!'); else alert('Hello!');">Test Action</button>
  </div>
</body>
</html>
""".trimIndent()
        }
    }
}
