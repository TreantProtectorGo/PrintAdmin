package com.ethan.printadmin;

import com.ethan.printadmin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrintadminApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach
    void clearUsers() { users.deleteAll(); }

    @Test
    void createsAndListsPersistedUser() throws Exception {
        mvc.perform(post("/users").contentType("application/json")
                .content("{\"name\":\" Ethan \",\"monthlyQuota\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ethan"));
        assertThat(users.count()).isEqualTo(1);
        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ethan"))
                .andExpect(jsonPath("$[0].monthlyQuota").value(100));
    }

    @Test
    void emptyDatabaseReturnsEmptyList() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void invalidRequestsDoNotWriteRows() throws Exception {
        for (String body : new String[] {
                "{\"name\":\" \",\"monthlyQuota\":100}",
                "{\"name\":\"Ethan\",\"monthlyQuota\":-1}",
                "{\"name\":\"Ethan\"}",
                "{\"name\":\"Ethan\",\"monthlyQuota\":null}",
                "{\"name\":\"" + "a".repeat(101) + "\",\"monthlyQuota\":100}"}) {
            mvc.perform(post("/users").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }
        assertThat(users.count()).isZero();
    }

    @Test
    void acceptsZeroQuota() throws Exception {
        mvc.perform(post("/users").contentType("application/json")
                .content("{\"name\":\"Ethan\",\"monthlyQuota\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monthlyQuota").value(0));
    }

    @Test
    void malformedJsonReturnsReadableError() throws Exception {
        mvc.perform(post("/users").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
        assertThat(users.count()).isZero();
    }
}
