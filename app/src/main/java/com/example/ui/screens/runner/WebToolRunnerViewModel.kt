/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.runner

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Tool
import com.example.data.repository.ToolRepository
import com.example.engine.ConsoleLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RunnerUiState(
    val tool: Tool? = null,
    val isLoading: Boolean = true,
    val progress: Int = 0,
    val isFullscreen: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isConsoleOpen: Boolean = false,
    val consoleLogs: List<ConsoleLogEntry> = emptyList(),
    val statusToast: String? = null
)

class WebToolRunnerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ToolRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ToolRepository(db.toolDao(), application)
    }

    private val _uiState = MutableStateFlow(RunnerUiState())
    val uiState: StateFlow<RunnerUiState> = _uiState.asStateFlow()

    fun loadTool(toolId: Long) {
        viewModelScope.launch {
            val tool = repository.getToolByIdDirect(toolId)
            if (tool != null) {
                // Update last accessed timestamp
                repository.recordToolAccess(toolId)
                _uiState.value = _uiState.value.copy(
                    tool = tool,
                    isLoading = false
                )
            }
        }
    }

    fun updateProgress(progress: Int) {
        _uiState.value = _uiState.value.copy(
            progress = progress,
            isLoading = progress < 100
        )
    }

    fun addConsoleLog(log: ConsoleLogEntry) {
        val current = _uiState.value.consoleLogs.toMutableList()
        current.add(0, log) // newest first
        if (current.size > 150) current.removeAt(current.size - 1)
        _uiState.value = _uiState.value.copy(consoleLogs = current)
    }

    fun clearConsoleLogs() {
        _uiState.value = _uiState.value.copy(consoleLogs = emptyList())
    }

    fun toggleConsole() {
        _uiState.value = _uiState.value.copy(isConsoleOpen = !_uiState.value.isConsoleOpen)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun toggleDesktopMode() {
        _uiState.value = _uiState.value.copy(isDesktopMode = !_uiState.value.isDesktopMode)
    }

    fun saveCapturedThumbnail(bitmap: Bitmap) {
        val toolId = _uiState.value.tool?.id ?: return
        viewModelScope.launch {
            repository.saveThumbnail(toolId, bitmap)
        }
    }

    fun showToast(msg: String) {
        _uiState.value = _uiState.value.copy(statusToast = msg)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(statusToast = null)
    }
}
