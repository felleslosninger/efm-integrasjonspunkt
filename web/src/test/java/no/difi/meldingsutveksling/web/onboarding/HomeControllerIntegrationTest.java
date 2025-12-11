package no.difi.meldingsutveksling.web.onboarding;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.inject.Inject;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerIntegrationTest {

    @Inject
    MockMvc mockMvc;

    @Test
    void indexRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                // Assert some domain-specific content the controller/template produces
                .andExpect(content().string(containsString("Integrasjonspunkt v4")))
                .andExpect(content().string(containsString("DPO")))
                .andExpect(content().string(containsString("DPV")))
                .andExpect(content().string(containsString("DPI")));
    }

}
