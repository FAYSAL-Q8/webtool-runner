/*
 * Author: Faysal Alibrahim
 * GitHub: https://github.com/FAYSAL-Q8
 */
package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun sampleTools_areValid() {
    val tools = com.example.engine.SampleTools.getDefaultTools()
    assertTrue(tools.isEmpty() || tools.all { it.htmlCode.isNotBlank() })
  }

  @Test
  fun testExtractSingleHtml() {
    val html = "<!DOCTYPE html><html><head><title>My Fancy Tool</title></head><body><h1>Hello</h1></body></html>"
    val stream = java.io.ByteArrayInputStream(html.toByteArray(Charsets.UTF_8))
    val result = com.example.engine.ZipToolManager.extractSingleHtml(stream, "my_fancy_tool.html")
    assertEquals("My Fancy Tool", result.toolName)
    assertEquals(html, result.htmlCode)
  }

  @Test
  fun testProjectFileOperations() {
    val tempDir = java.io.File.createTempFile("test_proj_", "").apply {
      delete()
      mkdirs()
    }
    try {
      assertTrue(com.example.engine.ZipToolManager.createNewFile(tempDir, "index.html", "<h1>Test</h1>"))
      assertTrue(com.example.engine.ZipToolManager.createNewFolder(tempDir, "css"))
      assertTrue(com.example.engine.ZipToolManager.createNewFile(tempDir, "css/styles.css", "body { color: red; }"))

      val files = com.example.engine.ZipToolManager.listProjectFiles(tempDir)
      assertEquals(3, files.size)

      val readContent = com.example.engine.ZipToolManager.readFileContent(tempDir, "css/styles.css")
      assertEquals("body { color: red; }", readContent)

      assertTrue(com.example.engine.ZipToolManager.deleteFileOrFolder(tempDir, "css/styles.css"))
      val filesAfterDelete = com.example.engine.ZipToolManager.listProjectFiles(tempDir)
      assertEquals(2, filesAfterDelete.size)
    } finally {
      tempDir.deleteRecursively()
    }
  }
}
