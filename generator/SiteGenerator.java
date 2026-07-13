import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small static-site generator — the Java replacement for Jekyll.
 *
 * It reads plain-text/Markdown sources from content/, pours them into the
 * templates/base.html shell, and writes ready-to-serve HTML to the repo root
 * and posts/ folder.
 *
 * Run from the repo root:
 *   javac -d generator/out generator/SiteGenerator.java
 *   java  -cp generator/out SiteGenerator
 */
public class SiteGenerator {

    private static final String CONTENT = "content";
    private static final String TEMPLATES = "templates";
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** A single blog post loaded from content/posts/. */
    private static final class Post {
        String title;
        String slug;
        LocalDate date;
        String displayDate;
        String body;      // raw Markdown
        String excerpt;   // plain-text summary
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> site = readConfig(Paths.get(CONTENT, "site.txt"));
        String template = readFile(Paths.get(TEMPLATES, "base.html"));
        String author = site.getOrDefault("author", "");
        String siteTitle = site.getOrDefault("title", "");
        int year = Year.now().getValue();

        List<Post> posts = loadPosts();
        // Newest first.
        posts.sort((a, b) -> b.date.compareTo(a.date));

        // ---- Home page ----
        String homeMd = readFile(Paths.get(CONTENT, "pages", "home.md"));
        StringBuilder home = new StringBuilder();
        home.append("<section class=\"intro\">\n").append(mdToHtml(homeMd)).append("</section>\n");
        home.append(renderProjects());
        home.append(renderBlogList(posts));
        writePage("index.html", "", template, site, siteTitle, home.toString(),
                year, author, "index.html");

        // ---- About page ----
        String aboutMd = readFile(Paths.get(CONTENT, "pages", "about.md"));
        String about = "<article class=\"page\">\n" + mdToHtml(aboutMd) + "</article>\n";
        writePage("about.html", "", template, site, "About · " + siteTitle, about,
                year, author, "about.html");

        // ---- Individual posts ----
        Files.createDirectories(Paths.get("posts"));
        for (Post p : posts) {
            writePage("posts/" + p.slug + ".html", "../", template, site,
                    p.title + " · " + siteTitle, renderPost(p), year, author, null);
        }

        System.out.println("Generated index.html, about.html, and " + posts.size() + " post(s).");
    }

    // ---------------------------------------------------------------- loading

    private static Map<String, String> readConfig(Path path) throws IOException {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (!Files.exists(path)) return map;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int eq = t.indexOf('=');
            if (eq > 0) map.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
        }
        return map;
    }

    private static List<Post> loadPosts() throws IOException {
        List<Post> posts = new ArrayList<Post>();
        File dir = new File(CONTENT, "posts");
        File[] files = dir.listFiles(new java.io.FilenameFilter() {
            public boolean accept(File d, String name) { return name.endsWith(".md"); }
        });
        if (files == null) return posts;
        for (File f : files) {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            Map<String, String> fm = new LinkedHashMap<String, String>();
            int idx = 0;
            if (!lines.isEmpty() && lines.get(0).trim().equals("---")) {
                idx = 1;
                while (idx < lines.size() && !lines.get(idx).trim().equals("---")) {
                    String l = lines.get(idx);
                    int c = l.indexOf(':');
                    if (c > 0) fm.put(l.substring(0, c).trim(), l.substring(c + 1).trim());
                    idx++;
                }
                idx++; // skip the closing "---"
            }
            StringBuilder body = new StringBuilder();
            for (int i = idx; i < lines.size(); i++) body.append(lines.get(i)).append('\n');

            Post p = new Post();
            String fileName = f.getName().replaceAll("\\.md$", "");
            p.slug = fileName.replaceFirst("^\\d{4}-\\d{2}-\\d{2}-", "");
            p.title = fm.containsKey("title") ? fm.get("title") : p.slug;
            String dateStr = fm.containsKey("date") ? fm.get("date") : "";
            p.date = parseDate(dateStr);
            p.displayDate = p.date.format(DISPLAY);
            p.body = body.toString();
            p.excerpt = excerpt(p.body);
            posts.add(p);
        }
        return posts;
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.substring(0, Math.min(10, s.length())));
        } catch (RuntimeException e) {
            return LocalDate.of(1970, 1, 1);
        }
    }

    // ------------------------------------------------------------- rendering

    private static String renderProjects() throws IOException {
        Path file = Paths.get(CONTENT, "projects.txt");
        if (!Files.exists(file)) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"projects\">\n<h2>Projects</h2>\n<ul class=\"card-list\">\n");
        boolean any = false;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            String[] parts = t.split("\\|");
            String name = parts.length > 0 ? parts[0].trim() : "";
            String url = parts.length > 1 ? parts[1].trim() : "#";
            String desc = parts.length > 2 ? parts[2].trim() : "";
            sb.append("<li class=\"card\"><a href=\"").append(esc(url)).append("\">")
              .append(esc(name)).append("</a>");
            if (!desc.isEmpty()) sb.append(" — ").append(esc(desc));
            sb.append("</li>\n");
            any = true;
        }
        sb.append("</ul>\n</section>\n");
        return any ? sb.toString() : "";
    }

    private static String renderBlogList(List<Post> posts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"blog\">\n<h2>Blog</h2>\n");
        sb.append("<input id=\"post-search\" class=\"search\" type=\"search\" ")
          .append("placeholder=\"Search posts…\" aria-label=\"Search posts\">\n");
        sb.append("<ul class=\"post-list\">\n");
        for (Post p : posts) {
            String data = esc((p.title + " " + p.excerpt).toLowerCase(Locale.ENGLISH));
            sb.append("<li class=\"post-item\" data-search=\"").append(data).append("\">");
            sb.append("<a class=\"post-link\" href=\"posts/").append(p.slug).append(".html\">")
              .append(esc(p.title)).append("</a>");
            sb.append("<time class=\"post-date\">").append(esc(p.displayDate)).append("</time>");
            if (!p.excerpt.isEmpty()) {
                sb.append("<p class=\"post-excerpt\">").append(esc(p.excerpt)).append("</p>");
            }
            sb.append("</li>\n");
        }
        sb.append("</ul>\n<p class=\"no-results\" hidden>No posts match your search.</p>\n</section>\n");
        return sb.toString();
    }

    private static String renderPost(Post p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<article class=\"post\">\n");
        sb.append("<h1>").append(esc(p.title)).append("</h1>\n");
        sb.append("<time class=\"post-date\">").append(esc(p.displayDate)).append("</time>\n");
        sb.append(mdToHtml(p.body));
        sb.append("<p class=\"back\"><a href=\"../index.html\">← Back home</a></p>\n");
        sb.append("</article>\n");
        return sb.toString();
    }

    private static String nav(String base, String active) {
        String[][] items = {{"Home", "index.html"}, {"About", "about.html"}};
        StringBuilder sb = new StringBuilder();
        for (String[] it : items) {
            String cls = it[1].equals(active) ? " class=\"active\"" : "";
            sb.append("<a href=\"").append(base).append(it[1]).append("\"").append(cls)
              .append(">").append(it[0]).append("</a>\n");
        }
        return sb.toString();
    }

    private static void writePage(String outPath, String base, String template,
                                  Map<String, String> site, String pageTitle,
                                  String content, int year, String author,
                                  String activeNav) throws IOException {
        String html = template
                .replace("{{page_title}}", esc(pageTitle))
                .replace("{{description}}", esc(site.getOrDefault("description", "")))
                .replace("{{base}}", base)
                .replace("{{site_title}}", esc(site.getOrDefault("title", "")))
                .replace("{{nav}}", nav(base, activeNav))
                .replace("{{content}}", content)
                .replace("{{year}}", String.valueOf(year))
                .replace("{{author}}", esc(author));
        Files.write(Paths.get(outPath), html.getBytes(StandardCharsets.UTF_8));
    }

    // ----------------------------------------------------------- Markdown

    /** Converts a supported subset of Markdown into HTML. */
    private static String mdToHtml(String md) {
        String[] lines = md.replace("\r\n", "\n").split("\n", -1);
        StringBuilder out = new StringBuilder();
        List<String> para = new ArrayList<String>();
        List<String> list = new ArrayList<String>();
        List<String> quote = new ArrayList<String>();
        List<String> code = new ArrayList<String>();
        boolean inCode = false;

        for (String line : lines) {
            String t = line.trim();

            if (inCode) {
                if (t.startsWith("```")) {
                    out.append("<pre><code>");
                    for (String cl : code) out.append(esc(cl)).append('\n');
                    out.append("</code></pre>\n");
                    code.clear();
                    inCode = false;
                } else {
                    code.add(line);
                }
                continue;
            }

            if (t.startsWith("```")) {
                flushPara(out, para); flushList(out, list); flushQuote(out, quote);
                inCode = true;
                continue;
            }
            if (t.isEmpty()) {
                flushPara(out, para); flushList(out, list); flushQuote(out, quote);
                continue;
            }
            if (t.startsWith("#")) {
                flushPara(out, para); flushList(out, list); flushQuote(out, quote);
                int level = 0;
                while (level < t.length() && t.charAt(level) == '#') level++;
                if (level > 6) level = 6;
                String text = t.substring(level).trim();
                out.append("<h").append(level).append(">").append(inline(text))
                   .append("</h").append(level).append(">\n");
                continue;
            }
            if (t.equals(">") || t.startsWith("> ")) {
                flushPara(out, para); flushList(out, list);
                quote.add(t.length() > 1 ? t.substring(1).trim() : "");
                continue;
            }
            if (t.startsWith("- ")) {
                flushPara(out, para); flushQuote(out, quote);
                list.add(t.substring(2).trim());
                continue;
            }
            // Regular paragraph text.
            flushList(out, list); flushQuote(out, quote);
            para.add(t);
        }

        if (inCode) { // tolerate an unterminated code fence
            out.append("<pre><code>");
            for (String cl : code) out.append(esc(cl)).append('\n');
            out.append("</code></pre>\n");
        }
        flushPara(out, para); flushList(out, list); flushQuote(out, quote);
        return out.toString();
    }

    private static void flushPara(StringBuilder out, List<String> para) {
        if (para.isEmpty()) return;
        out.append("<p>").append(inline(String.join(" ", para))).append("</p>\n");
        para.clear();
    }

    private static void flushList(StringBuilder out, List<String> list) {
        if (list.isEmpty()) return;
        out.append("<ul>\n");
        for (String item : list) out.append("<li>").append(inline(item)).append("</li>\n");
        out.append("</ul>\n");
        list.clear();
    }

    private static void flushQuote(StringBuilder out, List<String> quote) {
        if (quote.isEmpty()) return;
        out.append("<blockquote>").append(inline(String.join(" ", quote))).append("</blockquote>\n");
        quote.clear();
    }

    private static final Pattern CODE_SPAN = Pattern.compile("`([^`]+)`");

    // A delimiter that cannot appear in Markdown source, used to shield inline
    // code spans while the other inline rules run.
    private static final String MARK = " ";

    /** Handles inline formatting: code spans, links, bold, and italic. */
    private static String inline(String s) {
        s = esc(s);

        // Protect inline code so its contents aren't touched by the rules below.
        List<String> codes = new ArrayList<String>();
        Matcher m = CODE_SPAN.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            codes.add(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(MARK + (codes.size() - 1) + MARK));
        }
        m.appendTail(sb);
        s = sb.toString();

        s = s.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        s = s.replaceAll("\\*([^*]+)\\*", "<em>$1</em>");

        for (int i = 0; i < codes.size(); i++) {
            s = s.replace(MARK + i + MARK, "<code>" + codes.get(i) + "</code>");
        }
        return s;
    }

    /** First paragraph as a short plain-text summary. */
    private static String excerpt(String body) {
        for (String block : body.split("\\r?\\n\\r?\\n")) {
            String t = block.trim();
            if (t.isEmpty() || t.startsWith("#") || t.startsWith("```")
                    || t.startsWith(">") || t.startsWith("-")) continue;
            t = t.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                 .replaceAll("[*`]", "")
                 .replaceAll("\\s+", " ")
                 .trim();
            if (t.length() > 150) t = t.substring(0, 147).trim() + "…";
            return t;
        }
        return "";
    }

    /** Escapes characters that are special in HTML text and attributes. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String readFile(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
