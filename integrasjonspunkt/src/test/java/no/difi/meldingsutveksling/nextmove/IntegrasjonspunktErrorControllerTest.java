package no.difi.meldingsutveksling.nextmove;

import jakarta.servlet.RequestDispatcher;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
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
import no.difi.meldingsutveksling.oauth2.Oauth2ClientSecurityConfig;
import no.difi.meldingsutveksling.web.IntegrasjonspunktErrorController;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

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

@Import({
        FixedClockConfig.class,
        JacksonConfig.class,
        JacksonMockitoConfig.class,
        IntegrasjonspunktHandlerExceptionResolver.class,
        ValidationConfig.class})
@WebMvcTest({Oauth2ClientSecurityConfig.class, IntegrasjonspunktErrorController.class})
@AutoConfigureMoveRestDocs
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
@ComponentScan(basePackageClasses = WebhookFilterParser.class)
public class IntegrasjonspunktErrorControllerTest {

    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";

    @Autowired private MockMvc mvc;
    @Autowired private Validator validator;
    @MockitoBean IntegrasjonspunktProperties properties;

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
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 204,
                          "error" : "No Content",
                          "exception" : "no.difi.meldingsutveksling.exceptions.NoContentException",
                          "message" : "No content",
                          "path" : "/api/statuses/peek"
                        }\
                        """))
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
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 400,
                          "error" : "Bad Request",
                          "exception" : "javax.validation.ConstraintViolationException",
                          "message" : "standardBusinessDocumentHeader.documentIdentification.type: Must be a NextMove message type",
                          "path" : "/api/messages/out",
                          "errors" : [ {
                            "codes" : [ "MessageType" ],
                            "defaultMessage" : "Must be a NextMove message type",
                            "objectName" : "standardBusinessDocumentHeader.documentIdentification.type",
                            "field" : "standardBusinessDocumentHeader.documentIdentification.type",
                            "rejectedValue" : "strange",
                            "bindingFailure" : false,
                            "code" : "MessageType"
                          } ]
                        }\
                        """))
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
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 400,
                          "error" : "Bad Request",
                          "exception" : "no.difi.meldingsutveksling.exceptions.ServiceNotEnabledException",
                          "message" : "Service DPI is not enabled",
                          "path" : "/api/messages/out"
                        }\
                        """))
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
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Subscription with id = %d was not found".formatted(id))
                        .requestAttr(ERROR_ATTRIBUTE, new SubscriptionNotFoundException(id)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 404,
                          "error" : "Not Found",
                          "exception" : "no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException",
                          "message" : "Subscription with id = 84 was not found",
                          "path" : "/api/subscription/84"
                        }\
                        """))
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
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 400,
                          "error" : "Bad Request",
                          "exception" : "javax.validation.ConstraintViolationException",
                          "message" : "event: 'nonexistent' is not expected. Allowed values are: [all, status]",
                          "path" : "/api/subscriptions",
                          "errors" : [ {
                            "codes" : [ "OneOf" ],
                            "defaultMessage" : "'nonexistent' is not expected. Allowed values are: [all, status]",
                            "objectName" : "event",
                            "field" : "event",
                            "rejectedValue" : "nonexistent",
                            "bindingFailure" : false,
                            "code" : "OneOf"
                          } ]
                        }\
                        """))
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
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Conversation with id = %d was not found".formatted(id))
                        .requestAttr(ERROR_ATTRIBUTE, new ConversationNotFoundException(id)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 404,
                          "error" : "Not Found",
                          "exception" : "no.difi.meldingsutveksling.exceptions.ConversationNotFoundException",
                          "message" : "Conversation with id = 49 was not found",
                          "path" : "/api/conversations/49"
                        }\
                        """))
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
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Conversation with messageId = %s was not found".formatted(messageId))
                        .requestAttr(ERROR_ATTRIBUTE, new ConversationNotFoundException(messageId)
                        )
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 404,
                          "error" : "Not Found",
                          "exception" : "no.difi.meldingsutveksling.exceptions.ConversationNotFoundException",
                          "message" : "Conversation with messageId = df64afa1-83f4-497c-ae94-db22108801b9 was not found",
                          "path" : "/api/conversations/messageId/df64afa1-83f4-497c-ae94-db22108801b9"
                        }\
                        """))
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
                .andExpect(content().json("""
                        {
                          "timestamp" : "2019-03-25T12:38:23+01:00",
                          "status" : 204,
                          "error" : "No Content",
                          "exception" : "no.difi.meldingsutveksling.exceptions.NoContentException",
                          "message" : "No content",
                          "path" : "/api/messages/in/peek"
                        }\
                        """))
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