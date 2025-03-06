package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktHandlerExceptionResolver;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.NoContentException;
import no.difi.meldingsutveksling.exceptions.ServiceNotEnabledException;
import no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException;
import no.difi.meldingsutveksling.web.IntegrasjonspunktErrorController;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import javax.servlet.RequestDispatcher;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.Set;

import static no.difi.meldingsutveksling.nextmove.ConversationTestData.dpoConversation;
import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.errorFieldDescriptors;
import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.getDefaultHeaderDescriptors;
import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.ARKIVMELDING_MESSAGE_DATA;
import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.getInputSbd;
import static no.difi.meldingsutveksling.nextmove.SubscriptionTestData.incomingMessages;
import static no.difi.meldingsutveksling.nextmove.SubscriptionTestData.incomingMessagesInput;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@Import({
        FixedClockConfig.class,
        JacksonConfig.class,
        JacksonMockitoConfig.class,
        IntegrasjonspunktHandlerExceptionResolver.class,
        ValidationConfig.class})
@WebMvcTest(IntegrasjonspunktErrorController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
@ComponentScan(basePackageClasses = WebhookFilterParser.class)
public class IntegrasjonspunktErrorControllerTest {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    @Autowired private MockMvc mvc;
    @Autowired private Validator validator;
    @MockBean IntegrasjonspunktProperties properties;

    @BeforeEach
    public void before() {
        given(properties.getOrg()).willReturn(
                new IntegrasjonspunktProperties.Organization()
                        .setNumber("910077473")
        );
    }

    @Test
    public void statusesNoContent() throws Exception {

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 204)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/statuses/peek")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "No content")
                        .requestAttr(ERROR_ATTRIBUTE, new NoContentException())
                        .accept(MediaType.APPLICATION_JSON)
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
    public void createMessageConstraintViolation() throws Exception {
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
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 400,\n" +
                        "  \"error\" : \"Bad Request\",\n" +
                        "  \"exception\" : \"javax.validation.ConstraintViolationException\",\n" +
                        "  \"message\" : \"standardBusinessDocumentHeader.documentIdentification.type: Must be a NextMove message type\",\n" +
                        "  \"path\" : \"/api/messages/out\",\n" +
                        "  \"errors\" : [ {\n" +
                        "    \"codes\" : [ \"MessageType\" ],\n" +
                        "    \"defaultMessage\" : \"Must be a NextMove message type\",\n" +
                        "    \"objectName\" : \"standardBusinessDocumentHeader.documentIdentification.type\",\n" +
                        "    \"field\" : \"standardBusinessDocumentHeader.documentIdentification.type\",\n" +
                        "    \"rejectedValue\" : \"strange\",\n" +
                        "    \"bindingFailure\" : false,\n" +
                        "    \"code\" : \"MessageType\"\n" +
                        "  } ]\n" +
                        "}"))
                .andDo(document("messages/out/create/constraint-violation",
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
                        .accept(MediaType.APPLICATION_JSON)
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
                .andDo(document("messages/out/create/service-not-enabled",
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
    public void subscriptionNotFound() throws Exception {
        Long id = incomingMessages().getId();

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/subscription/" + id)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.format("Subscription with id = %d was not found", id))
                        .requestAttr(ERROR_ATTRIBUTE, new SubscriptionNotFoundException(id)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 404,\n" +
                        "  \"error\" : \"Not Found\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException\",\n" +
                        "  \"message\" : \"Subscription with id = 84 was not found\",\n" +
                        "  \"path\" : \"/api/subscription/84\"\n" +
                        "}"))
                .andDo(document("subscriptions/get/not-found",
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
    public void createSubscriptionConstraintViolation() throws Exception {

        Subscription input = incomingMessagesInput()
                .setEvent("nonexistent");

        Set<ConstraintViolation<Subscription>> constraintViolations = validator.validate(input);
        ConstraintViolationException constraintViolationException = new ConstraintViolationException(constraintViolations);

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/subscriptions")
                        .requestAttr(ERROR_ATTRIBUTE, constraintViolationException)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 400,\n" +
                        "  \"error\" : \"Bad Request\",\n" +
                        "  \"exception\" : \"javax.validation.ConstraintViolationException\",\n" +
                        "  \"message\" : \"event: 'nonexistent' is not expected. Allowed values are: [all, status]\",\n" +
                        "  \"path\" : \"/api/subscriptions\",\n" +
                        "  \"errors\" : [ {\n" +
                        "    \"codes\" : [ \"OneOf\" ],\n" +
                        "    \"defaultMessage\" : \"'nonexistent' is not expected. Allowed values are: [all, status]\",\n" +
                        "    \"objectName\" : \"event\",\n" +
                        "    \"field\" : \"event\",\n" +
                        "    \"rejectedValue\" : \"nonexistent\",\n" +
                        "    \"bindingFailure\" : false,\n" +
                        "    \"code\" : \"OneOf\"\n" +
                        "  } ]\n" +
                        "}"))
                .andDo(document("subscriptions/create/constraint-violation",
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
    public void conversationNotFound() throws Exception {
        Long id = dpoConversation().getId();

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/conversations/" + id)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.format("Conversation with id = %d was not found", id))
                        .requestAttr(ERROR_ATTRIBUTE, new ConversationNotFoundException(id)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 404,\n" +
                        "  \"error\" : \"Not Found\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.ConversationNotFoundException\",\n" +
                        "  \"message\" : \"Conversation with id = 49 was not found\",\n" +
                        "  \"path\" : \"/api/conversations/49\"\n" +
                        "}"))
                .andDo(document("conversations/get/not-found",
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
    public void conversationBtMyessageIdNotFound() throws Exception {
        String messageId = "df64afa1-83f4-497c-ae94-db22108801b9";

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/conversations/messageId/" + messageId)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, String.format("Conversation with messageId = %s was not found", messageId))
                        .requestAttr(ERROR_ATTRIBUTE, new ConversationNotFoundException(messageId)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 404,\n" +
                        "  \"error\" : \"Not Found\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.ConversationNotFoundException\",\n" +
                        "  \"message\" : \"Conversation with messageId = df64afa1-83f4-497c-ae94-db22108801b9 was not found\",\n" +
                        "  \"path\" : \"/api/conversations/messageId/df64afa1-83f4-497c-ae94-db22108801b9\"\n" +
                        "}"))
                .andDo(document("conversations/get-by-message-id/not-found",
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
    public void messagesInNoContent() throws Exception {

        mvc.perform(
                get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 204)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/messages/in/peek")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "No content")
                        .requestAttr(ERROR_ATTRIBUTE, new NoContentException())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNoContent())
                .andExpect(content().json("{\n" +
                        "  \"timestamp\" : \"2019-03-25T12:38:23+01:00\",\n" +
                        "  \"status\" : 204,\n" +
                        "  \"error\" : \"No Content\",\n" +
                        "  \"exception\" : \"no.difi.meldingsutveksling.exceptions.NoContentException\",\n" +
                        "  \"message\" : \"No content\",\n" +
                        "  \"path\" : \"/api/messages/in/peek\"\n" +
                        "}"))
                .andDo(document("messages/in/peek/no-content",
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