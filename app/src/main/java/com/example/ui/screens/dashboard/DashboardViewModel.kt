/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Tool
import com.example.data.repository.ToolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tools: List<Tool> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedTab: String = "ALL",
    val searchQuery: String = "",
    val exportedJson: String? = null,
    val showBackupDialog: Boolean = false,
    val statusMessage: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ToolRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ToolRepository(db.toolDao(), application)
        viewModelScope.launch {
            repository.syncDefaultToolsIfNeeded()
        }
    }

    private val _selectedTab = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")
    private val _showBackupDialog = MutableStateFlow(false)
    private val _exportedJson = MutableStateFlow<String?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)

    // First, combine tools and filtering logic (4 flows)
    private val _filteredToolsFlow = combine(
        repository.allTools,
        repository.recentTools,
        _selectedTab,
        _searchQuery
    ) { all, recent, tab, query ->
        val baseList = when (tab) {
            "ALL" -> all
            "RECENT" -> recent
            else -> all.filter { it.group.equals(tab, ignoreCase = true) }
        }

        val filtered = if (query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true)
            }
        }
        Triple(filtered, tab, query)
    }

    // Then, combine into final DashboardUiState (5 flows)
    val uiState: StateFlow<DashboardUiState> = combine(
        _filteredToolsFlow,
        repository.distinctGroups,
        _showBackupDialog,
        _exportedJson,
        _statusMessage
    ) { (filtered, tab, query), groups, showBackup, exportJson, message ->
        DashboardUiState(
            tools = filtered,
            groups = groups,
            selectedTab = tab,
            searchQuery = query,
            showBackupDialog = showBackup,
            exportedJson = exportJson,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(tab: String) {
        _selectedTab.value = tab
    }

    fun deleteTool(toolId: Long) {
        viewModelScope.launch {
            repository.deleteToolById(toolId)
            _statusMessage.value = "Tool deleted"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun openBackupDialog() {
        viewModelScope.launch {
            val json = repository.exportToolsToJson()
            _exportedJson.value = json
            _showBackupDialog.value = true
        }
    }

    fun dismissBackupDialog() {
        _showBackupDialog.value = false
        _exportedJson.value = null
    }

    fun importToolsFromJson(json: String) {
        viewModelScope.launch {
            val count = repository.importToolsFromJson(json)
            _statusMessage.value = "Imported $count tools"
            _showBackupDialog.value = false
        }
    }

    fun importZipTool(inputStream: java.io.InputStream, suggestedName: String?) {
        viewModelScope.launch {
            try {
                val newId = repository.importZipTool(inputStream, suggestedName)
                _statusMessage.value = "Imported ZIP project successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to import ZIP: ${e.message}"
            }
        }
    }

    fun importHtmlTool(inputStream: java.io.InputStream, suggestedName: String?) {
        viewModelScope.launch {
            try {
                val newId = repository.importHtmlTool(inputStream, suggestedName)
                _statusMessage.value = "Imported HTML tool successfully"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to import HTML: ${e.message}"
            }
        }
    }

    fun exportToolAsZip(toolId: Long, outputStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.exportToolAsZip(toolId, outputStream)
            onComplete(success)
            if (success) {
                _statusMessage.value = "Tool exported as ZIP"
            } else {
                _statusMessage.value = "Failed to export ZIP"
            }
        }
    }

    fun reseedDefaults() {
        viewModelScope.launch {
            repository.reseedDefaultTools()
            _statusMessage.value = "Default tools restored"
        }
    }
}
