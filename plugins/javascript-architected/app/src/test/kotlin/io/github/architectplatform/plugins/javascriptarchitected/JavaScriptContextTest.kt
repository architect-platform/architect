package io.github.architectplatform.plugins.javascriptarchitected

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JavaScriptContextTest {

  @Test
  fun `default context should use npm and current directory`() {
    val context = JavaScriptContext()
    
    assertEquals("npm", context.packageManager)
    assertEquals(".", context.workingDirectory)
  }

  @Test
  fun `context should accept custom package manager`() {
    val context = JavaScriptContext(packageManager = "yarn")
    
    assertEquals("yarn", context.packageManager)
  }

  @Test
  fun `context should accept custom working directory`() {
    val context = JavaScriptContext(workingDirectory = "packages/frontend")
    
    assertEquals("packages/frontend", context.workingDirectory)
  }

  @Test
  fun `context should accept both custom values`() {
    val context = JavaScriptContext(
      packageManager = "pnpm",
      workingDirectory = "apps/web"
    )
    
    assertEquals("pnpm", context.packageManager)
    assertEquals("apps/web", context.workingDirectory)
  }
}
