import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'ShinKu',
  description: 'A modern, immersive, and intelligent manga reader for Android.',
  base: '/ShinKu/',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['link', { rel: 'icon', href: '/ShinKu/app-icon.png' }],
    ['meta', { name: 'theme-color', content: '#b71c1c' }]
  ],
  themeConfig: {
    logo: '/app-icon.png',
    siteTitle: 'ShinKu',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guides', link: '/guides/getting-started' },
      { text: 'FAQ & Help', link: '/faq/troubleshooting' },
      { text: 'Philosophy', link: '/about/philosophy' },
      { text: 'Support Us', link: '/about/donate' },
      {
        text: 'Download',
        link: 'https://github.com/Harrys-HQ/ShinKu/releases'
      }
    ],

    sidebar: [
      {
        text: '📖 User Guides',
        items: [
          { text: 'Getting Started', link: '/guides/getting-started' },
          { text: 'Extension Repositories', link: '/guides/extension-repositories' },
          { text: 'Library & Sanctuary Deck', link: '/guides/library' },
          { text: 'AI & Immersion Features', link: '/guides/ai-features' },
          { text: 'Reading Journey & Stats', link: '/guides/reading-journey' },
          { text: 'Tracking & Sync', link: '/guides/tracking' },
          { text: 'Source Migration', link: '/guides/source-migration' },
          { text: 'Backups & Restoration', link: '/guides/backups' }
        ]
      },
      {
        text: '❓ FAQ & Troubleshooting',
        items: [
          { text: 'Common Troubleshooting', link: '/faq/troubleshooting' },
          { text: 'Storage & Downloads', link: '/faq/storage' },
          { text: 'Library & Bulk Updates', link: '/faq/library' }
        ]
      },
      {
        text: '❤️ Project & Community',
        items: [
          { text: 'Development Philosophy & AI', link: '/about/philosophy' },
          { text: 'Support & Donate', link: '/about/donate' },
          { text: 'DMCA & Safe Harbor', link: '/legal/dmca' },
          { text: 'Privacy Policy', link: '/legal/privacy' }
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Harrys-HQ/ShinKu' },
      {
        icon: {
          svg: '<svg role="img" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><title>Ko-fi</title><path d="M23.881 8.948c-.773-4.085-4.859-4.593-4.859-4.593H.723c-.604 0-.679.798-.679.798s-.082 7.324-.022 11.822c.164 2.424 2.586 2.672 2.586 2.672s8.267-.023 11.966-.049c2.438-.426 2.683-2.566 2.658-3.734 4.352.24 7.422-2.831 6.649-6.916zm-11.062 3.511c-1.246 1.453-4.011 3.976-4.011 3.976s-.121.119-.31.023c-.076-.057-.108-.09-.108-.09-.443-.441-3.368-3.049-4.034-3.954-.709-.965-1.041-2.7-.091-3.71.951-1.01 3.005-1.086 4.363.407 0 0 1.565-1.782 3.468-.963 1.904.82 1.832 3.011.723 4.311zm6.173.478c-.928.116-1.682.028-1.682.028V7.284h1.77s1.971.551 1.971 2.638c0 1.914-1.131 2.905-2.059 3.013z"/></svg>'
        },
        link: 'https://ko-fi.com/B0B21RXKG4',
        ariaLabel: 'Support on Ko-fi'
      }
    ],

    search: {
      provider: 'local'
    },

    footer: {
      message: 'Released under Apache 2.0 & GPLv3. ShinKu is an independent, client-side open-source reader.',
      copyright: 'Copyright © 2024–present ShinKu Contributors'
    }
  }
})
