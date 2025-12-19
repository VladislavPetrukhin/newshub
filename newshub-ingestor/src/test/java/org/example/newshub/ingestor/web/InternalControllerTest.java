package org.example.newshub.ingestor.web;

import org.example.newshub.ingestor.service.IngestorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalController.class)
class InternalControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    IngestorService service;

    @Test
    void refresh_returns_json_summary() throws Exception {
        var run = new IngestorService.RefreshRun(
                "fetch-1",
                3,
                "src-bad: boom"
        );

        when(service.refreshOnce()).thenReturn(run);

        mvc.perform(post("/internal/refresh"))
                .andExpect(jsonPath("$.fetchId").value("fetch-1"))
                .andExpect(jsonPath("$.batches").value(3));
                }
}
