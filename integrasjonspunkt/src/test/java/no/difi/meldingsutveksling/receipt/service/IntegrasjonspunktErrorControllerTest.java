package no.difi.meldingsutveksling.receipt.service;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktHandlerExceptionResolver;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.exceptions.ServiceNotEnabledException;
import no.difi.meldingsutveksling.web.IntegrasjonspunktErrorController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.servlet.RequestDispatcher;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.errorFieldDescriptors;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.getDefaultHeaderDescriptors;
import static no.difi.meldingsutveksling.receipt.service.StandardBusinessDocumentTestData.ARKIVMELDING_MESSAGE_DATA;
import static no.difi.meldingsutveksling.receipt.service.StandardBusinessDocumentTestData.getInputSbd;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({
        FixedClockConfig.class,
        JacksonConfig.class,
        JacksonMockitoConfig.class,
        IntegrasjonspunktHandlerExceptionResolver.class,
        ValidationConfig.class})
@WebMvcTest(IntegrasjonspunktErrorController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
public class IntegrasjonspunktErrorControllerTest {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    @Autowired private MockMvc mvc;
    @Autowired private Validator validator;
    @MockBean IntegrasjonspunktProperties properties;

    @Before
    public void before() {
        given(properties.getOrg()).willReturn(
                new IntegrasjonspunktProperties.Organization()
                        .setNumber("910077473")
        );
    }

    @Test
    public void noContent() throws Exception {

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 204)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/statuses/peek")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "No content")
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
                        "  \"message\" : \"No content\",\n" +
                        "  \"path\" : \"/api/statuses/peek\"\n" +
                        "}"))
                .andDo(document("error/no-content",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        responseFields(
                                errorFieldDescriptors(false)
                        )
                        )
                );
    }

    @Test
    public void constraintValidation() throws Exception {

        StandardBusinessDocument input = getInputSbd(ARKIVMELDING_MESSAGE_DATA);

        input.getStandardBusinessDocumentHeader()
                .getDocumentIdentification()
                .setType("strange");

        Set<ConstraintViolation<StandardBusinessDocument>> constraintViolations = validator.validate(input);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(constraintViolations);

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/messages/out")
                        .requestAttr(ERROR_ATTRIBUTE, constraintViolationException)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 400,\n" +
                        "  \"error\" : \"Bad Request\",\n" +
                        "  \"exception\" : \"javax.validation.ConstraintViolationException\",\n" +
                        "  \"message\" : \"standardBusinessDocumentHeader.documentIdentification.type: Must be a NextMove document type\",\n" +
                        "  \"path\" : \"/api/messages/out\",\n" +
                        "  \"errors\" : [ {\n" +
                        "    \"codes\" : [ \"IsDocumentType\" ],\n" +
                        "    \"defaultMessage\" : \"Must be a NextMove document type\",\n" +
                        "    \"objectName\" : \"standardBusinessDocumentHeader.documentIdentification.type\",\n" +
                        "    \"field\" : \"standardBusinessDocumentHeader.documentIdentification.type\",\n" +
                        "    \"rejectedValue\" : \"strange\",\n" +
                        "    \"bindingFailure\" : false,\n" +
                        "    \"code\" : \"IsDocumentType\"\n" +
                        "  } ]\n" +
                        "}"))
                .andDo(document("/messages/out/create/constraint-validation",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        responseFields(
                                errorFieldDescriptors(true)
                        )
                        )
                );
    }

    @Test
    public void serviceNotEnabled() throws Exception {
        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/messages/out")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Service DPI is not enabled")
                        .requestAttr(ERROR_ATTRIBUTE, new ServiceNotEnabledException(ServiceIdentifier.DPI)
                        )
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 400,\n" +
                        "  \"error\" : \"Bad Request\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.ServiceNotEnabledException\",\n" +
                        "  \"message\" : \"Service DPI is not enabled\",\n" +
                        "  \"path\" : \"/api/messages/out\"\n" +
                        "}"))
                .andDo(document("/messages/out/create/service-not-enabled",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        responseFields(
                                errorFieldDescriptors(false)
                        )
                        )
                );
    }
}