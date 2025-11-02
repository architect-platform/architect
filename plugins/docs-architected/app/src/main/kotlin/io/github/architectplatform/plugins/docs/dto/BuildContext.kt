package io.github.architectplatform.plugins.docs.dto

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
 * @property siteName Documentation site name
 * @property siteDescription Documentation site description
 * @property siteAuthor Documentation site author
 * @property repoUrl Repository URL for documentation
 * @property repoName Repository name (e.g., "username/repo")
 * @property primaryColor Primary color for theme (e.g., "indigo", "blue")
 * @property accentColor Accent color for theme (e.g., "indigo", "blue")
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
    val siteName: String = "My Project Documentation",
    val siteDescription: String = "Project documentation",
    val siteAuthor: String = "Your Name",
    val repoUrl: String = "",
    val repoName: String = "",
    val primaryColor: String = "indigo",
    val accentColor: String = "indigo"
)
