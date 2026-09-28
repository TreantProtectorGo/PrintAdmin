package com.ethan.printadmin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.core.env.Environment;
import javax.sql.DataSource;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class OpenApiTests {
    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;
    @Autowired Environment environment;

    @Test
    void describesAllEndpointsAndQuotaErrors() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PrintAdmin API"))
                .andExpect(jsonPath("$.paths['/users'].get").exists())
                .andExpect(jsonPath("$.paths['/users/{id}/usage'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/users/{id}/usage'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/users'].post").exists())
                .andExpect(jsonPath("$.paths['/printers'].get").exists())
                .andExpect(jsonPath("$.paths['/printers'].post").exists())
                .andExpect(jsonPath("$.paths['/print-jobs'].get").exists())
                .andExpect(jsonPath("$.paths['/print-jobs'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/print-jobs'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/print-jobs'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/print-jobs'].post.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePrintJobRequest.required",
                        org.hamcrest.Matchers.containsInAnyOrder("userId", "printerId", "pages")))
                .andExpect(jsonPath("$.paths['/print-jobs'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/PrintJobResponse"));
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    void usesRequestedDatabaseEngine() throws Exception {
        String expected = Arrays.asList(environment.getActiveProfiles()).contains("postgres-test")
                ? "PostgreSQL" : "H2";
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo(expected);
        }
    }
}
