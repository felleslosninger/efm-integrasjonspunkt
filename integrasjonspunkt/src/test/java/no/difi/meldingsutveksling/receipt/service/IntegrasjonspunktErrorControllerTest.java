package no.difi.meldingsutveksling.receipt.service;

import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktHandlerExceptionResolver;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.web.IntegrasjonspunktErrorController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.getDefaultHeaderDescriptors;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class, IntegrasjonspunktHandlerExceptionResolver.class})
@WebMvcTest(IntegrasjonspunktErrorController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
public class IntegrasjonspunktErrorControllerTest {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    @Autowired
    private MockMvc mvc;

    @Test
    public void noContent() throws Exception {
        mvc.perform(
                get("/error")
                        .requestAttr("javax.servlet.error.status_code", 204)
                        .requestAttr(ERROR_ATTRIBUTE, new NoContentException())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 204,\n" +
                        "  \"error\" : \"No Content\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.NoContentException\",\n" +
                        "  \"message\" : \"no.difi.meldingsutveksling.exceptions.NoContentException\"\n" +
                        "}"))
                .andDo(document("error/no-content",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        responseFields(
                                fieldWithPath("timestamp")
                                        .type(JsonFieldType.STRING)
                                        .description("Date and time for when the error occured."),
                                fieldWithPath("status")
                                        .type(JsonFieldType.NUMBER)
                                        .description("HTTP status code."),
                                fieldWithPath("error")
                                        .type(JsonFieldType.STRING)
                                        .description("Error description"),
                                fieldWithPath("exception")
                                        .optional()
                                        .type(JsonFieldType.STRING)
                                        .description("The java class of the Exception that was thrown"),
                                fieldWithPath("message")
                                        .type(JsonFieldType.STRING)
                                        .description("A message describing the error.")
                        )
                        )
                );
    }
}