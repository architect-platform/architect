package io.github.architectplatform.cli.command

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class HelpCommandHandlerTest {

  @Test
  fun `config help includes lint subcommand`() {
    val output = captureStdout {
      HelpCommandHandler().handle(listOf("help", "config"))
    }

    assertTrue(output.contains("lint               Lint config for deprecated/unknown entries"))
    assertTrue(output.contains("architect config lint"))
  }

  @Test
  fun `overview and secret help document secret management`() {
    val overview = captureStdout {
      HelpCommandHandler().handle(listOf("help"))
    }
    val secretHelp = captureStdout {
      HelpCommandHandler().handle(listOf("help", "secret"))
    }

    assertTrue(overview.contains("secret             Manage locally stored secrets"))
    assertTrue(secretHelp.contains("architect secret <subcommand>"))
    assertTrue(secretHelp.contains("environment.secret(name)"))
  }

  private fun captureStdout(block: () -> Unit): String {
    val original = System.out
    val output = ByteArrayOutputStream()
    System.setOut(PrintStream(output))
    try {
      block()
    } finally {
      System.setOut(original)
    }
    return output.toString()
  }
}
