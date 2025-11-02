module.exports = {
  title: 'My Project Documentation',
  description: 'Documentation built with VuePress',
  base: '/',
  
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
      { text: 'GitHub', link: 'https://github.com/your-username/your-repo' }
    ],
    
    sidebar: {
      '/guide/': [
        '',
        'getting-started',
      ]
    },
    
    repo: 'your-username/your-repo',
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
