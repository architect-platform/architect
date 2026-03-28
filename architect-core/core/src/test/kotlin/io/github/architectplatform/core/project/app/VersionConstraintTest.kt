package io.github.architectplatform.core.project.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VersionConstraintTest {
  @Test
  fun `single version matches exactly`() {
    val constraint = VersionConstraint.parse("1.2.3")
    assertNotNull(constraint)
    assertTrue(constraint.isSatisfiedBy("1.2.3"))
    assertFalse(constraint.isSatisfiedBy("1.2.4"))
  }

  @Test
  fun `range constraints apply all requirements`() {
    val constraint = VersionConstraint.parse(">=1.2.0 <2.0.0")
    assertNotNull(constraint)
    assertTrue(constraint.isSatisfiedBy("1.9.9"))
    assertFalse(constraint.isSatisfiedBy("2.0.0"))
  }

  @Test
  fun `operators respect equality`() {
    val constraint = VersionConstraint.parse("<=1.4.0")
    assertNotNull(constraint)
    assertTrue(constraint.isSatisfiedBy("1.4.0"))
    assertFalse(constraint.isSatisfiedBy("1.4.1"))
  }
}
