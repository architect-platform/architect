package io.github.architectplatform.intellij

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ArchitectTaskLineMarkerProviderTest : BasePlatformTestCase() {
  fun testShowsLineMarkerForTaskDefinitionsInsideArchitectTasksBlock() {
    val file = myFixture.configureByText(
      "architect.yml",
      """
        tasks:
          build-app:
            description: Build the project
      """.trimIndent(),
    )

    val info = PsiTreeUtil.collectElements(file) { true }
      .firstNotNullOfOrNull { ArchitectTaskLineMarkerProvider().getInfo(it) }

    assertNotNull(info)
  }

  fun testDoesNotShowLineMarkerOutsideTasksBlock() {
    val file = myFixture.configureByText(
      "architect.yml",
      """
        scripts:
          scripts:
            hello:
              description: Says hello
      """.trimIndent(),
    )

    val info = PsiTreeUtil.collectElements(file) { true }
      .firstNotNullOfOrNull { ArchitectTaskLineMarkerProvider().getInfo(it) }

    assertNull(info)
  }

  fun testDoesNotShowLineMarkerForNonArchitectFiles() {
    val file = myFixture.configureByText(
      "application.yml",
      """
        tasks:
          build-app:
            description: Build the project
      """.trimIndent(),
    )

    val info = PsiTreeUtil.collectElements(file) { true }
      .firstNotNullOfOrNull { ArchitectTaskLineMarkerProvider().getInfo(it) }

    assertNull(info)
  }
}
