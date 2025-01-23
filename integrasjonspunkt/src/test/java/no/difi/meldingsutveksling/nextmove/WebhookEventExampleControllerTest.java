package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.oauth2.Oauth2ClientSecurityConfig;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.getDefaultHeaderDescriptors;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest({Oauth2ClientSecurityConfig.class, WebhookEventExampleController.class})
@AutoConfigureRestDocs(uriHost = "your.pushendpoint.com", uriPort = 80)
@ActiveProfiles("test")
@ComponentScan(basePackageClasses = WebhookFilterParser.class)
public class WebhookEventExampleControllerTest {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private MockMvc mvc;

    @Test
    public void ping() throws Exception {
        mvc.perform(
                post("/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(WebhookEventExamples.ping())))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("events/ping",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestFields(
                                fieldWithPath("createdTs").description("Timestamp for when the event occurred."),
                                fieldWithPath("event").description("Type of event.")
                        ))
                );
    }

    @Test
    public void messageStatus() throws Exception {
        mvc.perform(
                post("/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(WebhookEventExamples.messageStatus()))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("events/message-status",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestFields(
                                fieldWithPath("createdTs")
                                        .description("Timestamp for when the event occurred."),
                                fieldWithPath("resource")
                                        .optional()
                                        .description("Type of resource. Currently there are only one possible value: messages"),
                                fieldWithPath("event")
                                        .description("Type of event. Possible values are: ping and status."),
                                fieldWithPath("messageId")
                                        .description("The messageId of the message that triggered the event."),
                                fieldWithPath("conversationId")
                                        .description("The conversationId of the message that triggered the event."),
                                fieldWithPath("direction")
                                        .description("Direction of the message that triggered the event. Possible values are INCOMING and OUTGOING."),
                                fieldWithPath("serviceIdentifier")
                                        .description("Service identifier of the message that triggered the event. Possible values"),
                                fieldWithPath("status")
                                        .description("Status of the message that triggered the event."),
                                fieldWithPath("description")
                                        .optional()
                                        .description("A more detailed description.")
                        ))
                );
    }
}
