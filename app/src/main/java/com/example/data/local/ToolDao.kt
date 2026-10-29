/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Tool
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {

    @Query("SELECT * FROM tools ORDER BY lastAccessedAt DESC")
    fun getAllTools(): Flow<List<Tool>>

    @Query("SELECT * FROM tools ORDER BY lastAccessedAt DESC")
    suspend fun getAllToolsDirect(): List<Tool>

    @Query("SELECT * FROM tools ORDER BY lastAccessedAt DESC LIMIT 10")
    fun getRecentTools(): Flow<List<Tool>>

    @Query("SELECT * FROM tools WHERE `group` = :group ORDER BY lastAccessedAt DESC")
    fun getToolsByGroup(group: String): Flow<List<Tool>>

    @Query("SELECT * FROM tools WHERE id = :id LIMIT 1")
    fun getToolById(id: Long): Flow<Tool?>

    @Query("SELECT * FROM tools WHERE id = :id LIMIT 1")
    suspend fun getToolByIdDirect(id: Long): Tool?

    @Query("SELECT DISTINCT `group` FROM tools WHERE `group` IS NOT NULL AND `group` != '' ORDER BY `group` ASC")
    fun getDistinctGroups(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: Tool): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<Tool>): List<Long>

    @Update
    suspend fun updateTool(tool: Tool)

    @Query("UPDATE tools SET thumbnailPath = :thumbnailPath WHERE id = :id")
    suspend fun updateThumbnail(id: Long, thumbnailPath: String)

    @Query("UPDATE tools SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Long, timestamp: Long)

    @Delete
    suspend fun deleteTool(tool: Tool)

    @Query("DELETE FROM tools WHERE id = :id")
    suspend fun deleteToolById(id: Long)

    @Query("DELETE FROM tools")
    suspend fun deleteAllTools()

    @Query("DELETE FROM tools WHERE isBuiltIn = 1")
    suspend fun deleteBuiltInTools()

    @Query("SELECT COUNT(*) FROM tools WHERE isBuiltIn = 1")
    suspend fun getBuiltInCount(): Int

    @Query("SELECT COUNT(*) FROM tools")
    suspend fun getToolsCount(): Int
}
