package org.example.newshub.web;

import org.example.newshub.model.Feed;
import org.example.newshub.model.FeedCategory;
import org.example.newshub.model.NewsItem;
import org.example.newshub.service.FeedRegistry;
import org.example.newshub.service.NewsService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlRenderer {

    private final FeedRegistry feedRegistry;
    private final NewsService newsService;

    public HtmlRenderer(FeedRegistry feedRegistry, NewsService newsService) {
        this.feedRegistry = feedRegistry;
        this.newsService = newsService;
    }

    public String renderIndex(NewsService.Page page, String sort, String sourceId) {
        NewsService.Stats stats = newsService.stats();

        Set<String> activeSourceIds = newsService.uniqueSourceIds();
        List<String> activeSourceList = new ArrayList<>(activeSourceIds);
        activeSourceList.sort(String::compareTo);

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append("<title>newshub - rss агрегатор</title>")
                .append("<link rel='stylesheet' href='/app.css'>")
                .append("<script src='/app.js' defer></script>")
                .append("</head><body><div class='container'>");

        // header
        html.append("<header class='header'>")
                .append("<h1>newshub</h1>")
                .append("<div class='subtitle'>агрегатор rss новостей с выбором источников</div>")
                .append("</header>");

        // control panel
        html.append("<div class='control-panel'>")
                .append("<div class='control-group'>")
                .append("<form action='/fetch' method='post' id='refresh-form' style='margin:0'>")
                .append("<button type='submit' class='btn' id='refresh-btn'>")
                .append(stats.lastFetchTime() == null ? "загрузить новости" : "обновить новости")
                .append("</button>");

        if (stats.lastFetchTime() != null) {
            html.append("<small class='muted'>")
                    .append("последнее обновление: ").append(formatTimeAgo(stats.lastFetchTime()))
                    .append("</small>");
        }

        html.append("</form>")
                .append("<a href='/stats' class='btn btn-success'>статистика</a>")
                .append("</div>")
                .append("<div class='control-group'>")
                .append("<form action='/search' method='get' class='search-form'>")
                .append("<input type='text' name='q' placeholder='поиск новостей...'>")
                .append("<button type='submit' class='btn'>поиск</button>")
                .append("</form></div>")
                .append("<div class='control-group'>")
                .append("<a href='/select-sources' class='btn btn-info'>выбрать источники</a>")
                .append("<a href='/add-custom-feed' class='btn btn-success'>добавить свой rss</a>")
                .append("</div></div>");

        // stats cards
        html.append("<div class='stats'>")
                .append(statCard(stats.totalNews(), "всего новостей"))
                .append(statCard(stats.selectedFeeds(), "выбрано источников"))
                .append(statCard(activeSourceIds.size(), "активных источников"))
                .append(statCard(stats.newCount(), "новых"))
                .append("</div>");

        // filters
        html.append("<div class='filters'>")
                .append("<span class='filters-label'>сортировка:</span>")
                .append(filterTag("/?sort=date" + (sourceId != null ? "&amp;source=" + escAttr(sourceId) : ""), "по дате", "date".equals(sort)))
                .append(filterTag("/?sort=title" + (sourceId != null ? "&amp;source=" + escAttr(sourceId) : ""), "по названию", "title".equals(sort)))
                .append(filterTag("/?sort=source" + (sourceId != null ? "&amp;source=" + escAttr(sourceId) : ""), "по источнику", "source".equals(sort)))
                .append("<div class='spacer'></div>")
                .append("<span class='filters-label'>источники:</span>")
                .append(filterTag("/?sort=" + escAttr(sort), "все", sourceId == null || sourceId.isBlank()));

        for (String id : activeSourceList) {
            String name = feedRegistry.findById(id).map(Feed::name).orElse(id);
            html.append(filterTag("/?sort=" + escAttr(sort) + "&amp;source=" + escAttr(id), name, id.equals(sourceId)));
        }

        html.append("</div>");

        // news grid
        html.append("<div class='news-grid'>");

        if (page.items().isEmpty()) {
            html.append("<div class='empty'>")
                    .append("<h2>новостей нет</h2>")
                    .append("<p>нажмите кнопку загрузить новости чтобы получить новости из выбранных источников</p>")
                    .append("</div>");
        } else {
            for (NewsItem item : page.items()) {
                boolean isNew = !item.seen();

                html.append("<div class='news-card").append(isNew ? " new" : "").append("'>");
                if (isNew) html.append("<div class='new-badge'>новое</div>");

                html.append("<div class='news-header'>")
                        .append("<h3 class='news-title'><a href='").append(escAttr(item.link())).append("' target='_blank' rel='noopener'>")
                        .append(escapeHtml(or(item.title(), "без названия"))).append("</a></h3>")
                        .append("<div class='news-meta'><span class='news-source'>")
                        .append(escapeHtml(or(item.sourceName(), "неизвестно")))
                        .append("</span><span>")
                        .append(escapeHtml(or(item.pubDateRaw(), "")))
                        .append("</span></div></div>")
                        .append("<div class='news-body'><p class='news-desc'>")
                        .append(truncateText(escapeHtml(or(item.description(), "нет описания")), 200))
                        .append("</p></div></div>");
            }
        }

        html.append("</div>");

        // pagination
        if (page.totalPages() > 1) {
            html.append("<div class='pagination'>");
            for (int i = 1; i <= page.totalPages(); i++) {
                String url = "/?page=" + i +
                        (sort != null ? "&amp;sort=" + escAttr(sort) : "") +
                        (sourceId != null ? "&amp;source=" + escAttr(sourceId) : "");
                html.append("<a href='").append(url).append("' class='page-btn")
                        .append(i == page.page() ? " active" : "")
                        .append("'>").append(i).append("</a>");
            }
            html.append("</div>");
        }

        // footer
        html.append("<div class='footer'>")
                .append("newshub v2.2 • выбрано источников: ").append(feedRegistry.selectedIds().size()).append(" • ")
                .append("<a href='/clear-cache'>очистить просмотренные</a>")
                .append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    public String renderSelectSources() {
        List<Feed> all = feedRegistry.allFeedsInOrder();
        Set<String> selected = feedRegistry.selectedIds();

        Map<FeedCategory, List<Feed>> byCat = new LinkedHashMap<>();
        byCat.put(FeedCategory.RUSSIAN, new ArrayList<>());
        byCat.put(FeedCategory.INTERNATIONAL, new ArrayList<>());
        byCat.put(FeedCategory.BELARUS, new ArrayList<>());
        byCat.put(FeedCategory.KAZAKHSTAN, new ArrayList<>());
        byCat.put(FeedCategory.CUSTOM, new ArrayList<>());

        for (Feed f : all) {
            byCat.computeIfAbsent(f.category(), k -> new ArrayList<>()).add(f);
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>выбор источников</title>")
                .append("<style>")
                .append("body{font-family:Arial,sans-serif;padding:25px;max-width:1200px;margin:0 auto;background:#f5f7fa;color:#333}")
                .append("h1{color:#2c3e50}")
                .append(".back-btn{display:inline-block;margin-bottom:15px;text-decoration:none;color:#3498db;font-weight:600}")
                .append(".sources-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:15px;margin-bottom:25px}")
                .append(".source-group{background:#fff;padding:15px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.05);border:1px solid #eee}")
                .append(".group-title{font-size:1.1em;margin-bottom:12px;color:#2c3e50;border-bottom:1px solid #eee;padding-bottom:8px}")
                .append(".source-item{margin:8px 0;padding:8px;border-radius:6px;background:#f8f9fa;display:flex;align-items:center;gap:10px}")
                .append(".source-item:hover{background:#e9ecef}")
                .append(".source-name{flex:1}")
                .append("input[type='checkbox']{transform:scale(1.2)}")
                .append(".btn{background:#3498db;color:#fff;border:none;padding:12px 24px;border-radius:6px;cursor:pointer;font-size:16px;font-weight:500;margin-right:8px}")
                .append("</style>")
                .append("</head><body>")
                .append("<a href='/' class='back-btn'>на главную</a>")
                .append("<h1>выбор источников новостей</h1>")
                .append("<p>отметьте источники которые хотите использовать. выбрано: <strong id='selected-count'>")
                .append(selected.size())
                .append("</strong></p>")
                .append("<form action='/update-sources' method='post' id='sources-form'>")
                .append("<div class='sources-grid'>");

        for (Map.Entry<FeedCategory, List<Feed>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;

            html.append("<div class='source-group'>")
                    .append("<div class='group-title'>")
                    .append(e.getKey().title()).append(" (").append(e.getValue().size()).append(")")
                    .append("</div>");

            for (Feed f : e.getValue()) {
                boolean checked = selected.contains(f.id());
                html.append("<div class='source-item'>")
                        .append("<input type='checkbox' id='").append(escAttr(f.id()))
                        .append("' name='source' value='").append(escAttr(f.id()))
                        .append("' onchange='updateCounter()' ").append(checked ? "checked" : "").append(">")
                        .append("<label for='").append(escAttr(f.id())).append("' class='source-name'>")
                        .append(escapeHtml(f.name()))
                        .append("</label>")
                        .append("</div>");
            }

            html.append("</div>");
        }

        html.append("</div>")
                .append("<div style='margin-top: 25px;'>")
                .append("<button type='submit' class='btn'>сохранить выбор</button>")
                .append("<button type='button' class='btn' onclick='selectAll()'>выбрать все</button>")
                .append("<button type='button' class='btn' onclick='deselectAll()'>очистить все</button>")
                .append("</div>")
                .append("</form>")
                .append("<script>")
                .append("function updateCounter(){const checked=document.querySelectorAll('input[type=checkbox]:checked').length;document.getElementById('selected-count').textContent=checked;}")
                .append("function selectAll(){document.querySelectorAll('input[type=checkbox]').forEach(cb=>cb.checked=true);updateCounter();}")
                .append("function deselectAll(){document.querySelectorAll('input[type=checkbox]').forEach(cb=>cb.checked=false);updateCounter();}")
                .append("</script>")
                .append("</body></html>");

        return html.toString();
    }

    public String renderAddCustomFeedPage() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>добавить свой rss</title>" +
                "<style>body{font-family:Arial,sans-serif;padding:30px;max-width:600px;margin:0 auto;background:#f5f7fa;color:#333}" +
                "h1{color:#2c3e50}.back-btn{display:inline-block;margin-bottom:15px;text-decoration:none;color:#3498db;font-weight:600}" +
                "form{background:#fff;padding:25px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.05)}" +
                "input{width:100%;padding:12px;margin:10px 0;border:1px solid #ddd;border-radius:6px}" +
                ".btn{background:#3498db;color:#fff;border:none;padding:12px 24px;border-radius:6px;cursor:pointer;font-size:16px}</style>" +
                "</head><body>" +
                "<a href='/' class='back-btn'>на главную</a>" +
                "<h1>добавить свой rss источник</h1>" +
                "<form action='/add-custom-feed' method='post'>" +
                "<input type='text' name='name' placeholder='название источника' required>" +
                "<input type='url' name='url' placeholder='https://example.com/rss' pattern='https?://.+' required>" +
                "<button type='submit' class='btn'>добавить источник</button>" +
                "</form></body></html>";
    }

    public String renderStats(NewsService.Stats stats) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>статистика newshub</title>")
                .append("<style>body{font-family:Arial,sans-serif;padding:25px;max-width:1000px;margin:0 auto;background:#f5f7fa;color:#333}")
                .append("h1{color:#2c3e50}.back-btn{display:inline-block;margin-bottom:15px;text-decoration:none;color:#3498db;font-weight:600}")
                .append(".stats-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:15px}")
                .append(".stat-box{background:#fff;padding:18px;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.05);border:1px solid #eee}")
                .append(".stat-title{font-size:1.1em;margin-bottom:12px;color:#2c3e50;border-bottom:1px solid #eee;padding-bottom:8px}")
                .append(".source-item{display:flex;justify-content:space-between;margin:6px 0;padding:4px 0;border-bottom:1px solid #f8f9fa}")
                .append("</style></head><body>")
                .append("<a href='/' class='back-btn'>на главную</a>")
                .append("<h1>статистика newshub</h1>")
                .append("<div class='stats-grid'>");

        html.append("<div class='stat-box'><div class='stat-title'>общая статистика</div>")
                .append("<p><strong>всего новостей:</strong> ").append(stats.totalNews()).append("</p>")
                .append("<p><strong>доступно источников:</strong> ").append(stats.totalFeeds()).append("</p>")
                .append("<p><strong>выбрано источников:</strong> ").append(stats.selectedFeeds()).append("</p>")
                .append("<p><strong>новых/непросмотренных:</strong> ").append(stats.newCount()).append("</p>");

        if (stats.lastFetchTime() != null) {
            html.append("<p><strong>последнее обновление:</strong> ").append(formatTimeAgo(stats.lastFetchTime())).append("</p>");
        }

        html.append("</div>");

        html.append("<div class='stat-box'><div class='stat-title'>распределение по источникам</div>");
        stats.newsBySource().entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(20)
                .forEach(e -> html.append("<div class='source-item'><span>")
                        .append(escapeHtml(e.getKey()))
                        .append("</span><span><strong>")
                        .append(e.getValue())
                        .append("</strong></span></div>"));
        html.append("</div>");

        html.append("<div class='stat-box'><div class='stat-title'>выбранные источники</div>");
        for (String id : feedRegistry.selectedIds()) {
            String name = feedRegistry.findById(id)
                    .map(Feed::name)
                    .orElseGet(() -> newsService.lookupSourceName(id).orElse(id));
            html.append("<div class='source-item'><span>")
                    .append(escapeHtml(name))
                    .append("</span><span><strong>")
                    .append(id)
                    .append("</strong></span></div>");
        }
        html.append("</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    public String renderSearch(String query, List<NewsItem> results) {
        String q = query == null ? "" : query.trim();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>результаты поиска</title>")
                .append("<style>body{font-family:Arial,sans-serif;padding:25px;max-width:1000px;margin:0 auto;background:#f5f7fa;color:#333}")
                .append(".back-btn{display:inline-block;margin-bottom:15px;text-decoration:none;color:#3498db;font-weight:600}")
                .append(".result-count{color:#7f8c8d;margin-bottom:20px}")
                .append(".news-item{border:1px solid #eee;padding:18px;margin:12px 0;border-radius:6px;background:#fff}")
                .append(".news-title{font-size:1.2em;margin-bottom:8px}")
                .append(".news-desc{color:#34495e}")
                .append("</style></head><body>")
                .append("<a href='/' class='back-btn'>на главную</a>")
                .append("<h1>результаты поиска</h1>")
                .append("<div class='result-count'>найдено <strong>")
                .append(results.size())
                .append("</strong> новостей по запросу: <strong>")
                .append(escapeHtml(q))
                .append("</strong></div>");

        if (results.isEmpty()) {
            html.append("<p style='color:#7f8c8d;text-align:center;padding:30px;'>ничего не найдено</p>");
        } else {
            for (NewsItem it : results) {
                String title = highlightText(or(it.title(), ""), q);
                String desc  = highlightText(truncateText(or(it.description(), ""), 300), q);

                html.append("<div class='news-item'>")
                        .append("<h3 class='news-title'><a href='").append(escAttr(or(it.link(), "#")))
                        .append("' target='_blank' rel='noopener'>")
                        .append(title)
                        .append("</a></h3>")
                        .append("<div style='color:#7f8c8d;font-size:0.85em;margin-bottom:8px;'>")
                        .append(escapeHtml(or(it.sourceName(), ""))).append(" • ").append(escapeHtml(or(it.pubDateRaw(), "")))
                        .append("</div>")
                        .append("<p class='news-desc'>").append(desc).append("</p>")
                        .append("</div>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String statCard(int value, String label) {
        return "<div class='stat-card'><div class='stat-number'>" + value + "</div><div class='stat-label'>" + escapeHtml(label) + "</div></div>";
    }

    private static String filterTag(String href, String label, boolean active) {
        return "<a href='" + href + "' class='filter-tag" + (active ? " active" : "") + "'>" + escapeHtml(label) + "</a>";
    }

    private static String formatTimeAgo(Instant time) {
        Duration d = Duration.between(time, Instant.now());
        if (d.toMinutes() < 1) return "только что";
        if (d.toMinutes() < 60) return d.toMinutes() + " мин назад";
        if (d.toHours() < 24) return d.toHours() + " ч назад";
        return d.toDays() + " дн назад";
    }

    private static String truncateText(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private static String highlightText(String text, String query) {
        String safeText = escapeHtml(or(text, ""));
        String q = or(query, "").trim();
        if (q.isEmpty()) return safeText;

        Pattern p = Pattern.compile(Pattern.quote(escapeHtml(q)), Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(safeText);

        StringBuffer out = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(out, "<mark>" + m.group() + "</mark>");
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String escAttr(String s) {
        return escapeHtml(or(s, "")).replace(" ", "%20");
    }

    private static String or(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    // очень простой экранировщик: достаточно для UI без внешних шаблонов
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
