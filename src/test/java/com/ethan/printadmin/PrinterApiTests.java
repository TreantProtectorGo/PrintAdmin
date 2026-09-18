package com.ethan.printadmin;

import com.ethan.printadmin.repository.PrinterRepository;
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
class PrinterApiTests {
    @Autowired MockMvc mvc;
    @Autowired PrinterRepository printers;

    @BeforeEach
    void clearPrinters() { printers.deleteAll(); }

    @Test
    void createsAndListsSavedPrintersInIdOrder() throws Exception {
        mvc.perform(post("/printers").contentType("application/json")
                .content("{\"name\":\" Staff Printer \",\"location\":\" 2/F \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Staff Printer"))
                .andExpect(jsonPath("$.location").value("2/F"));
        mvc.perform(post("/printers").contentType("application/json")
                .content("{\"name\":\"Library Printer\",\"location\":\"1/F\"}"))
                .andExpect(status().isCreated());
        assertThat(printers.count()).isEqualTo(2);
        assertThat(printers.findAll()).extracting("location").containsExactlyInAnyOrder("2/F", "1/F");
        mvc.perform(get("/printers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Staff Printer"))
                .andExpect(jsonPath("$[1].name").value("Library Printer"));
    }

    @Test
    void emptyDatabaseReturnsEmptyList() throws Exception {
        mvc.perform(get("/printers")).andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void rejectsMissingBlankAndOverlongFieldsWithoutSaving() throws Exception {
        for (String body : new String[] {
                "{}",
                "{\"name\":null,\"location\":null}",
                "{\"name\":\" \",\"location\":\"2/F\"}",
                "{\"name\":\"Printer\",\"location\":\" \"}",
                "{\"name\":\"" + "a".repeat(101) + "\",\"location\":\"2/F\"}",
                "{\"name\":\"Printer\",\"location\":\"" + "a".repeat(201) + "\"}"}) {
            mvc.perform(post("/printers").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").exists());
        }
        assertThat(printers.count()).isZero();
    }

    @Test
    void acceptsMaximumFieldLengths() throws Exception {
        mvc.perform(post("/printers").contentType("application/json")
                .content("{\"name\":\"" + "a".repeat(100)
                        + "\",\"location\":\"" + "b".repeat(200) + "\"}"))
                .andExpect(status().isCreated());
        assertThat(printers.count()).isEqualTo(1);
    }

    @Test
    void malformedJsonReturnsGeneralError() throws Exception {
        mvc.perform(post("/printers").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Provide a valid JSON object with the required fields and correct value types."));
        assertThat(printers.count()).isZero();
    }
}
