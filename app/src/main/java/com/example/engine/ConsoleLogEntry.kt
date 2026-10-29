/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example.engine

data class ConsoleLogEntry(
    val level: String, // "LOG", "WARN", "ERROR", "INFO"
    val message: String,
    val lineNumber: Int,
    val sourceId: String,
    val timestamp: Long = System.currentTimeMillis()
)
