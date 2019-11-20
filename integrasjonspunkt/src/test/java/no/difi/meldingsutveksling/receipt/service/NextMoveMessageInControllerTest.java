package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveInMessageQueryInput;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusQueryInput;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.receipt.service.MessageStatusTestData.messageStatus1;
import static no.difi.meldingsutveksling.receipt.service.MessageStatusTestData.messageStatus2;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.*;
import static no.difi.meldingsutveksling.receipt.service.StandardBusinessDocumentTestData.ARKIVMELDING_SBD;
import static no.difi.meldingsutveksling.receipt.service.StandardBusinessDocumentTestData.DPI_DIGITAL_SBD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, ValidationConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(NextMoveMessageInController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
public class NextMoveMessageInControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NextMoveMessageInService messageService;
    @MockBean private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock private IntegrasjonspunktProperties.Organization organization;

    @Before
    public void before() {
        given(organization.getNumber()).willReturn("910077473");
        given(integrasjonspunktProperties.getOrg()).willReturn(organization);
    }

    @Test
    public void find() throws Exception {
        given(messageService.findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<StandardBusinessDocument> content = Arrays.asList(ARKIVMELDING_SBD, DPI_DIGITAL_SBD);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/messages/in")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/find",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("Filter on messageId."),
                                parameterWithName("conversationId").optional().description("Filter on conversationId."),
                                parameterWithName("receiverIdentifier").optional().description("Filter on receiverIdentifier."),
                                parameterWithName("senderIdentifier").optional().description("Filter on senderIdentifier."),
                                parameterWithName("serviceIdentifier").optional().description(String.format("Filter on serviceIdentifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                                parameterWithName("process").optional().description("Filter on process.")
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("content[].standardBusinessDocumentHeader."))
                                .and(subsectionWithPath("content[].arkivmelding").description("The DPO business message").optional())
                                .and(arkivmeldingMessageDescriptors("content[].arkivmelding."))
                                .and(subsectionWithPath("content[].digital.").description("The digital DPI business message").optional())
                                .and(dpiDigitalMessageDescriptors("content[].digital."))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(messageService).findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSearch() throws Exception {
        given(messageService.findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<StandardBusinessDocument> content = Collections.singletonList(ARKIVMELDING_SBD);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/messages/in")
                        .param("serviceIdentifier", "DPO")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/find/search"));

        verify(messageService).findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSorting() throws Exception {
        given(messageService.findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<StandardBusinessDocument> content = Arrays.asList(ARKIVMELDING_SBD, DPI_DIGITAL_SBD);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/messages/in")
                        .param("sort", "lastUpdated,asc")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/find/sorting"));

        verify(messageService).findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findPaging() throws Exception {
        given(messageService.findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<StandardBusinessDocument> content = Collections.singletonList(ARKIVMELDING_SBD);
                    return new PageImpl<>(content, invocation.getArgument(1), 31L);
                });

        mvc.perform(
                get("/api/messages/in")
                        .param("page", "3")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/find/paging"));

        verify(messageService).findMessages(any(NextMoveInMessageQueryInput.class), any(Pageable.class));
    }

    @Test
    public void peek() throws Exception {
        given(messageService.peek(any(NextMoveInMessageQueryInput.class))).willReturn(ARKIVMELDING_SBD);

        mvc.perform(
                get("/api/messages/in/peek")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/peek",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("Filter on messageId."),
                                parameterWithName("conversationId").optional().description("Filter on conversationId."),
                                parameterWithName("receiverIdentifier").optional().description("Filter on receiverIdentifier."),
                                parameterWithName("senderIdentifier").optional().description("Filter on senderIdentifier."),
                                parameterWithName("serviceIdentifier").optional().description(String.format("Filter on serviceIdentifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                                parameterWithName("process").optional().description("Filter on process.")
                        ),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                .and(subsectionWithPath("arkivmelding").description("The DPO business message").optional())
                                .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).peek(any(NextMoveInMessageQueryInput.class));
    }

    @Test
    public void peekDPO() throws Exception {
        given(messageService.peek(any(NextMoveInMessageQueryInput.class))).willReturn(ARKIVMELDING_SBD);

        mvc.perform(
                get("/api/messages/in/peek")
                        .param("serviceIdentifier", ServiceIdentifier.DPO.getFullname())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/in/peek/dpo",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("serviceIdentifier").optional().description(String.format("Filter on serviceIdentifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                        ),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                .and(subsectionWithPath("arkivmelding").description("The DPO business message").optional())
                                .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).peek(any(NextMoveInMessageQueryInput.class));
    }

}