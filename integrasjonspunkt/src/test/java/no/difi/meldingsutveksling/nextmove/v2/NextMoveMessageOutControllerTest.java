package no.difi.meldingsutveksling.nextmove.v2;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(NextMoveMessageOutController.class)
@Import(RestResponseStatusExceptionResolver.class)
@Ignore
public class NextMoveMessageOutControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StandardBusinessDocumentRepository standardBusinessDocumentRepository;

    @Test
    public void createResourceWithoutReceiverShouldFail() throws Exception {
        mvc.perform(post("/api/message/out")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{}"))
                .andExpect(status().isOk());
    }
}
