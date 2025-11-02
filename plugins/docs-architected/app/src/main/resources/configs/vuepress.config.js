module.exports = {
  title: '{{siteName}}',
  description: '{{siteDescription}}',
  base: '/',
  
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
      { text: 'GitHub', link: '{{repoUrl}}' }
    ],
    
    sidebar: {
      '/guide/': [
        '',
        'getting-started',
      ]
    },
    
    repo: '{{repoName}}',
    repoLabel: 'GitHub',
    docsDir: 'docs',
    editLinks: true,
    editLinkText: 'Edit this page on GitHub',
    lastUpdated: 'Last Updated',
    
    search: true,
    searchMaxSuggestions: 10
  },
  
  markdown: {
    lineNumbers: true
  },
  
  plugins: [
    '@vuepress/back-to-top',
    '@vuepress/medium-zoom',
  ]
}
