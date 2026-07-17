# profile

My personal website and blog — built with **HTML, CSS, JavaScript**, and a
small **Java** static-site generator (no Jekyll).

**Live site:** https://wendyqian11.github.io/profile/

## How it works

You edit simple text files in `content/`, run the Java generator, and it writes
plain HTML that GitHub Pages serves as-is (the `.nojekyll` file tells GitHub not
to run Jekyll).

```
content/            <- the only folder you normally edit
  site.txt          <- site title, author, description, GitHub username
  projects.txt      <- your projects (one per line: Title | link | description)
  pages/home.md     <- the intro shown on the home page
  pages/about.md    <- the About page
  pages/investment.md <- the Investment page (Stock, Crypto, Gold and Silver)
  posts/*.md        <- blog posts (filename: YYYY-MM-DD-title.md)

templates/base.html <- the HTML shell every page is poured into
assets/css/style.css<- styling (includes a light/dark theme)
assets/js/main.js   <- dark-mode toggle + live blog search
generator/SiteGenerator.java <- the Java generator (Jekyll's replacement)

index.html, about.html, investment.html, posts/*.html <- GENERATED - do not edit by hand
```

## Build the site

Requires Java (JDK 8 or newer).

```
./build.sh
```

This compiles and runs the generator, producing `index.html`, `about.html`, and
one page per post. Open `index.html` in a browser to preview.

## Add a new blog post

1. Create `content/posts/2026-08-01-my-post-title.md`
2. Start it with this block, then write your post in Markdown below it:

```
---
title: My post title
date: 2026-08-01
---
```

3. Run `./build.sh`, then commit and push. Your site updates in ~1 minute.

## Publish a change

```
./build.sh
git add -A
git commit -m "Update site"
git push
```

## Supported Markdown

Headings, bold, italic, inline code, links, bullet lists, blockquotes, and
fenced code blocks.
