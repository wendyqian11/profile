# xiaoyuqian.github.io

My personal website and blog, published for free with
[GitHub Pages](https://pages.github.com/).

**Live site:** https://wendyqian11.github.io/xiaoyuqian.github.io/

## How it works

- Pages like the home page and About page are Markdown/HTML files in the root.
- Blog posts live in the `_posts/` folder, named `YYYY-MM-DD-title.md`.
- Site-wide settings (your name, links, etc.) live in `_config.yml`.
- When you push a change to GitHub, the site rebuilds automatically in ~1 minute.

## Editing

The easiest way to edit is right on GitHub.com:

1. Open the file you want to change.
2. Click the pencil ✏️ icon.
3. Make your edit and click **Commit changes**.

## Add a new blog post

Create a new file in `_posts/` named like `2026-08-01-my-post-title.md` and start
it with this block:

```
---
layout: post
title: "My post title"
date: 2026-08-01 10:00:00 +0000
---
```

Then write your post below it in Markdown.

## Preview locally (optional, advanced)

```bash
bundle install
bundle exec jekyll serve
# then open http://localhost:4000
```
