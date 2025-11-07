package io.github.architectplatform.plugins.docs.dto

/**
 * Configuration for a subproject/component documentation.
 *
 * @property name Name of the component (e.g., "architect-api")
 * @property path Relative path to the component directory
 * @property docsPath Relative path to the docs folder within the component (default: "docs")
 */
data class ComponentDocs(
    val name: String = "",
    val path: String = "",
    val docsPath: String = "docs"
)

/**
 * Configuration context for documentation building.
 *
 * This class defines the configuration for building documentation from markdown sources.
 *
 * @property enabled Whether documentation building is enabled
 * @property framework Documentation framework to use (mkdocs, docusaurus, vuepress, manual)
 * @property sourceDir Source directory containing documentation files
 * @property outputDir Output directory for built documentation
 * @property configFile Optional custom configuration file for the framework
 * @property installDeps Whether to install dependencies before building
 * @property mkdocsVersion Version of MkDocs to install (default: 1.5.3)
 * @property mkdocsMaterialVersion Version of MkDocs Material theme to install (default: 9.5.3)
 * @property mkdocsMonorepoVersion Version of mkdocs-monorepo-plugin to install (default: 1.0.5)
 * @property siteName Documentation site name
 * @property siteDescription Documentation site description
 * @property siteAuthor Documentation site author
 * @property repoUrl Repository URL for documentation
 * @property repoName Repository name (e.g., "username/repo")
 * @property primaryColor Primary color for theme (e.g., "indigo", "blue")
 * @property accentColor Accent color for theme (e.g., "indigo", "blue")
 * @property autoDiscoverComponents Whether to automatically discover components with docs folders (default: true)
 * @property componentPaths List of paths to search for components (default: [".", "plugins"])
 * @property components List of manual component/subproject documentation configurations (optional, auto-discovery used if empty)
 */
data class BuildContext(
    val enabled: Boolean = true,
    val framework: String = "mkdocs",
    val sourceDir: String = "docs",
    val outputDir: String = "site",
    val configFile: String = "",
    val installDeps: Boolean = true,
    val mkdocsVersion: String = "1.5.3",
    val mkdocsMaterialVersion: String = "9.5.3",
    val mkdocsMonorepoVersion: String = "1.0.5",
    val siteName: String = "My Project Documentation",
    val siteDescription: String = "Project documentation",
    val siteAuthor: String = "Your Name",
    val repoUrl: String = "",
    val repoName: String = "",
    val primaryColor: String = "indigo",
    val accentColor: String = "indigo",
    val autoDiscoverComponents: Boolean = true,
    val componentPaths: List<String> = listOf(".", "plugins"),
    val components: List<ComponentDocs> = emptyList()
)
