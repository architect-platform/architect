package io.github.architectplatform.plugins.docs.builders

import io.github.architectplatform.api.components.execution.CommandExecutor
import io.github.architectplatform.api.core.tasks.TaskResult
import io.github.architectplatform.plugins.docs.dto.BuildContext
import io.github.architectplatform.plugins.docs.dto.ComponentDocs
import java.io.File

/**
 * Documentation builder for Docusaurus (React-based documentation framework).
 * 
 * This builder:
 * - Ensures Node.js environment is ready
 * - Installs Docusaurus dependencies via npm/yarn/pnpm
 * - Generates docusaurus.config.js if needed
 * - Builds documentation using npm run build
 */
class DocusaurusBuilder(
    context: BuildContext,
    commandExecutor: CommandExecutor
) : NodeJsDocumentationBuilder(context, commandExecutor) {
    
    override fun getName(): String = "Docusaurus"
    
    /**
     * Generates docusaurus.config.js configuration if it doesn't exist.
     */
    override fun generateConfiguration(workingDir: File, components: List<ComponentDocs>): TaskResult {
        val configFile = File(workingDir, "docusaurus.config.js")
        
        return if (configFile.exists()) {
            TaskResult.success("Docusaurus configuration already exists")
        } else {
            try {
                // Generate a basic docusaurus.config.js
                val config = generateDocusaurusConfig()
                configFile.writeText(config)
                TaskResult.success("Generated Docusaurus configuration")
            } catch (e: Exception) {
                TaskResult.failure("Failed to generate Docusaurus configuration: ${e.message}")
            }
        }
    }
    
    /**
     * Builds documentation using npm run build.
     */
    override fun build(workingDir: File): TaskResult {
        return try {
            val packageLock = File(workingDir, "package-lock.json")
            val yarnLock = File(workingDir, "yarn.lock")
            val pnpmLock = File(workingDir, "pnpm-lock.yaml")
            
            val command = when {
                pnpmLock.exists() -> "pnpm build"
                yarnLock.exists() -> "yarn build"
                else -> "npm run build"
            }
            
            commandExecutor.execute(command, workingDir.toString())
            TaskResult.success("Built documentation with Docusaurus")
        } catch (e: Exception) {
            TaskResult.failure("Failed to build documentation: ${e.message}")
        }
    }
    
    /**
     * Generates a basic docusaurus.config.js configuration.
     */
    private fun generateDocusaurusConfig(): String {
        return """
// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: '${context.siteName}',
  tagline: '${context.siteDescription}',
  ${if (context.repoUrl.isNotEmpty()) "url: '${context.repoUrl}'," else "url: 'https://your-docusaurus-test-site.com',"}
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  
  ${if (context.repoUrl.isNotEmpty()) {
    val parts = context.repoUrl.removePrefix("https://github.com/").split("/")
    """
  organizationName: '${parts.getOrNull(0) ?: "facebook"}',
  projectName: '${parts.getOrNull(1) ?: "docusaurus"}',
    """.trimIndent()
  } else {
    """
  organizationName: 'facebook',
  projectName: 'docusaurus',
    """.trimIndent()
  }}

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          ${if (context.repoUrl.isNotEmpty()) "editUrl: '${context.repoUrl}/tree/main/'," else ""}
        },
        blog: {
          showReadingTime: true,
          ${if (context.repoUrl.isNotEmpty()) "editUrl: '${context.repoUrl}/tree/main/'," else ""}
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: '${context.siteName}',
        items: [
          {
            type: 'doc',
            docId: 'intro',
            position: 'left',
            label: 'Documentation',
          },
          ${if (context.repoUrl.isNotEmpty()) """
          {
            href: '${context.repoUrl}',
            label: 'GitHub',
            position: 'right',
          },
          """ else ""}
        ],
      },
      footer: {
        style: 'dark',
        copyright: `Copyright © ${'$'}{new Date().getFullYear()} ${context.siteAuthor}. Built with Docusaurus.`,
      },
      prism: {
        theme: require('prism-react-renderer/themes/github'),
        darkTheme: require('prism-react-renderer/themes/dracula'),
      },
    }),
};

module.exports = config;
""".trimIndent()
    }
}
