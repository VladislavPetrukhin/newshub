package org.example.newshub.web;

import org.example.newshub.service.FeedRegistry;
import org.example.newshub.service.KeywordService;
import org.example.newshub.service.NewsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NewsController.class)
class NewsControllerWebTest {

    @Autowired MockMvc mvc;

    @MockBean FeedRegistry feedRegistry;
    @MockBean NewsService newsService;
    @MockBean HtmlRenderer renderer;
    @MockBean KeywordService keywordService;

    @Test
    void fetch_redirects_without_any_js_alerts() throws Exception {
        mvc.perform(post("/fetch"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/?sort=date"));

        verify(newsService).keepOnlySelectedSources();
        verify(newsService).triggerRefresh();
    }

    @Test
    void index_calls_service_and_renders_html() throws Exception {
        var page = new NewsService.Page(List.of(), 1, 1, 0);
        when(newsService.list(any(), any(), any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(page);
        when(newsService.distinctCategories()).thenReturn(List.of("Tech", "World"));
        when(renderer.renderIndex(eq(page), any(), any(), any(), any(), anyBoolean(), anyList()))
                .thenReturn("<html>OK</html>");

        mvc.perform(get("/").param("page", "1").param("sort", "date"))
                .andExpect(status().isOk())
                .andExpect(content().string("<html>OK</html>"));

        verify(newsService).markSeen(ArgumentMatchers.eq(page.items()));
    }
}
