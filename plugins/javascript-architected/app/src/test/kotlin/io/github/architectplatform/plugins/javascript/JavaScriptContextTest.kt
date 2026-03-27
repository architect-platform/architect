package io.github.architectplatform.plugins.javascript

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JavaScriptContextTest {

  @Test
  fun `default context should use npm and current directory`() {
    val context = JavaScriptContext()

    assertEquals("npm", context.packageManager)
    assertEquals("auto", context.yarnMode)
    assertEquals(".", context.workingDirectory)
    assertEquals("public", context.publishAccess)
    assertEquals("patch", context.defaultVersionBump)
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
      packageManager = "pnpm"
    )

    assertEquals("pnpm", context.packageManager)
  }

  @Test
  fun `context should normalize package manager and yarn mode`() {
    val context = JavaScriptContext(packageManager = " Yarn ", yarnMode = " Berry ")

    assertEquals("yarn", context.normalizedPackageManager())
    assertEquals("berry", context.normalizedYarnMode())
  }
}
