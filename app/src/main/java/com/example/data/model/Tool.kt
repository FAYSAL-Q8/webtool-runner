/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tools")
data class Tool(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val group: String = "Utility",
    val htmlCode: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false,
    val projectType: String = PROJECT_TYPE_SINGLE,
    val projectDir: String? = null,
    val entryPoint: String = "index.html",
    val description: String = ""
) {
    companion object {
        const val PROJECT_TYPE_SINGLE = "SINGLE_FILE"
        const val PROJECT_TYPE_MULTI = "MULTI_FILE"
    }

    val isMultiFile: Boolean
        get() = projectType == PROJECT_TYPE_MULTI
}
