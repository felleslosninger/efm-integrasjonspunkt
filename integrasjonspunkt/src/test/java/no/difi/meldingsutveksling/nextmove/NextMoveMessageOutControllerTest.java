package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.MvcConfiguration;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveUploadedFile;
import no.difi.meldingsutveksling.nextmove.v2.OnBehalfOfNormalizer;
import no.difi.meldingsutveksling.oauth2.Oauth2ClientSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.*;
import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {
        Oauth2ClientSecurityConfig.class,
        FixedClockConfig.class,
        ValidationConfig.class,
        JacksonConfig.class,
        JacksonMockitoConfig.class,
        NextMoveMessageOutController.class,
        MvcConfiguration.class})
@WebMvcTest(NextMoveMessageOutController.class)
@AutoConfigureMoveRestDocs
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
class NextMoveMessageOutControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NextMoveMessageService messageService;

    @MockitoBean
    private OnBehalfOfNormalizer onBehalfOfNormalizer;

    @MockitoBean
    private ArkivmeldingUtil arkivmeldingUtil;

    @Mock
    private NextMoveOutMessage messageMock;
    @Captor
    private ArgumentCaptor<NextMoveUploadedFile> nextMoveUploadedFileArgumentCaptor;

    @AfterEach
    public void after() {
        Mockito.verifyNoMoreInteractions(messageService, onBehalfOfNormalizer);
        Mockito.validateMockitoUsage();
    }

    @Test
    void multipart() throws Exception {
        given(messageMock.getId()).willReturn(34L);
        given(messageService.createMessage(any(StandardBusinessDocument.class), anyList())).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(ARKIVMELDING_MESSAGE.getSbd());

        mvc.perform(
                        MockMvcRequestBuilders.multipart("/api/messages/out/multipart")
                                .file(new MockMultipartFile("sbd", null, MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(ARKIVMELDING_INPUT)))
                                .file(new MockMultipartFile("Before The Law", "before_the_law.txt", MediaType.TEXT_PLAIN_VALUE, "Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law...".getBytes(StandardCharsets.UTF_8)))
                                .accept(MediaType.APPLICATION_JSON)
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestParts(
                                        partWithName("sbd")
                                                .description("The Standard Business Document."),
                                        partWithName("Before The Law")
                                                .optional()
                                                .description("""
                                                        There can be zero or more attachments. The name of the part will be used as a title for the attachment.
                                                        The originalFilename will be used as the name of the file in the ASiC.
                                                        Please note that the Content-Type of the part must be set to the MIME-type of the attachment.""")
                                ),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(onBehalfOfNormalizer).normalize(any());
        verify(messageService).createMessage(any(StandardBusinessDocument.class), anyList());
        verify(messageService).sendMessage(34L);
    }

    @Test
    void createArkivmeldingMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(ARKIVMELDING_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(ARKIVMELDING_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(ARKIVMELDING_SBD)))
                .andDo(document("messages/out/create",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(arkivmeldingMessageDescriptors("arkivmelding.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void createDpiDigitalMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(DPI_DIGITAL_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(DPI_DIGITAL_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DPI_DIGITAL_SBD)))
                .andDo(document("messages/out/create-dpi-digital",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(dpiDigitalMessageDescriptors("digital.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(dpiDigitalMessageDescriptors("digital."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void createDigitalDpvMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(DIGITAL_DPV_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(DIGITAL_DPV_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DIGITAL_DPV_SBD)))
                .andDo(document("messages/out/create-digital-dpv",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(digitalDpvMessageDescriptors("digital_dpv.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(digitalDpvMessageDescriptors("digital_dpv."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void createDpiPrintMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(DPI_PRINT_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(DPI_PRINT_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(DPI_PRINT_SBD)))
                .andDo(document("messages/out/create-dpi-print",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(dpiPrintMessageDescriptors("print.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(dpiPrintMessageDescriptors("print."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void createInnsynskravMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(INNSYNSKRAV_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(INNSYNSKRAV_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(INNSYNSKRAV_SBD)))
                .andDo(document("messages/out/create-innsynskrav",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(innsynskravMessageDescriptors("innsynskrav.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(innsynskravMessageDescriptors("innsynskrav."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void createPubliseringMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(PUBLISERING_MESSAGE.getSbd());

        mvc.perform(
                        post("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(PUBLISERING_INPUT))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(PUBLISERING_SBD)))
                .andDo(document("messages/out/create-publisering",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                requestFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(publiseringMessageDescriptors("publisering.")),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(publiseringMessageDescriptors("publisering."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
        verify(onBehalfOfNormalizer).normalize(any());
    }

    @Test
    void find() throws Exception {
        given(messageService.findMessages(any(Predicate.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<NextMoveMessage> content = Arrays.asList(ARKIVMELDING_MESSAGE, DPI_DIGITAL_MESSAGE);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                        get("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/find",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                queryParameters(
                                        parameterWithName("conversationId").optional().description("Filter on conversationId"),
                                        parameterWithName("messageId").optional().description("Filter on messageId"),
                                        parameterWithName("processIdentifier").optional().description("Filter on processIdentifier"),
                                        parameterWithName("receiverIdentifier").optional().description("Filter on receiverIdentifier"),
                                        parameterWithName("senderIdentifier").optional().description("Filter on senderIdentifier"),
                                        parameterWithName("serviceIdentifier").optional().description("Filter on service identifier. Can be one of: %s".formatted(Arrays.stream(ServiceIdentifier.values())
                                                .map(Enum::name)
                                                .collect(Collectors.joining(", "))))
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

        verify(messageService).findMessages(any(Predicate.class), any(Pageable.class));
    }

    @Test
    void findDpo() throws Exception {
        given(messageService.findMessages(any(Predicate.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<NextMoveMessage> content = Collections.singletonList(ARKIVMELDING_MESSAGE);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                        get("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .param("serviceIdentifier", "DPO")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/find/dpo"));

        verify(messageService).findMessages(any(Predicate.class), any(Pageable.class));
    }

    @Test
    void findSorting() throws Exception {
        given(messageService.findMessages(any(), any()))
                .willAnswer(invocation -> {
                    List<NextMoveMessage> content = Arrays.asList(ARKIVMELDING_MESSAGE, DPI_DIGITAL_MESSAGE);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                        get("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .param("sort", "lastUpdated,asc")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/find/sorting"));

        verify(messageService).findMessages(any(), any());
    }

    @Test
    void findPaging() throws Exception {
        given(messageService.findMessages(any(), any()))
                .willAnswer(invocation -> {
                    List<NextMoveMessage> content = Collections.singletonList(DPI_DIGITAL_MESSAGE);
                    return new PageImpl<>(content, invocation.getArgument(1), 41L);
                });

        mvc.perform(
                        get("/api/messages/out")
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .param("page", "3")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/find/paging"));

        verify(messageService).findMessages(any(), any());
    }

    @Test
    void getMessage() throws Exception {
        given(messageService.getMessage(any())).willReturn(ARKIVMELDING_MESSAGE);

        mvc.perform(
                        get("/api/messages/out/{messageId}", ARKIVMELDING_MESSAGE_DATA.getMessageId())
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                queryParameters(
                                        parameterWithName("messageId").optional().description("The messageId")
                                ),
                                responseFields()
                                        .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                        .and(subsectionWithPath("arkivmelding").description("The DPO business message"))
                                        .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).getMessage(ARKIVMELDING_MESSAGE_DATA.getMessageId());
    }

    @Test
    void sendMessage() throws Exception {
        given(messageService.getMessage(any())).willReturn(ARKIVMELDING_MESSAGE);

        mvc.perform(
                        post("/api/messages/out/{messageId}", ARKIVMELDING_MESSAGE_DATA.getMessageId())
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/send",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                queryParameters(
                                        parameterWithName("messageId").optional().description("The messageId")
                                )
                        )
                );

        verify(messageService).getMessage(ARKIVMELDING_MESSAGE_DATA.getMessageId());
        verify(messageService).sendMessage(same(ARKIVMELDING_MESSAGE));
    }

    @Test
    void deleteMessage() throws Exception {
        mvc.perform(
                        delete("/api/messages/out/{messageId}", ARKIVMELDING_MESSAGE_DATA.getMessageId())
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(
                                        getDefaultHeaderDescriptors()
                                ),
                                queryParameters(
                                        parameterWithName("messageId").optional().description("The messageId")
                                )
                        )
                );

        verify(messageService).deleteMessage(ARKIVMELDING_MESSAGE_DATA.getMessageId());
    }

    @Test
    void uploadFile() throws Exception {
        given(messageService.getMessage(any())).willReturn(ARKIVMELDING_MESSAGE);

        mvc.perform(
                        put("/api/messages/out/{messageId}", ARKIVMELDING_MESSAGE_DATA.getMessageId())
                                .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpassword"))
                                .contentType(MediaType.TEXT_PLAIN)
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; name=Before The Law; filename=before_the_law.txt")
                                .accept(MediaType.APPLICATION_JSON)
                                .content("Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law...".getBytes(StandardCharsets.UTF_8))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/upload",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders()
                                .and(getDefaultHeaderDescriptors())
                                .and(headerWithName(HttpHeaders.CONTENT_TYPE).description("Content type of the attachment."),
                                        headerWithName(HttpHeaders.CONTENT_DISPOSITION).description("The title of the document. " +
                                                "The title can alternatively be specified using the name attribute of the Content-Disposition header. " +
                                                "The filename is specified in the filename attribute of this header.")
                                ),
                        queryParameters(
                                parameterWithName("messageId").optional().description("The messageId"),
                                parameterWithName("title").optional().description("The attachment title can alternatively be specified in this request parameter. " +
                                        "If not specified here, then the title is extracted from the Content-Disposition HTTP header.")
                        ))
                );

        verify(messageService).getMessage(ARKIVMELDING_MESSAGE_DATA.getMessageId());
        verify(messageService).addFile(same(ARKIVMELDING_MESSAGE), nextMoveUploadedFileArgumentCaptor.capture());

        NextMoveUploadedFile value = nextMoveUploadedFileArgumentCaptor.getValue();
        assertThat(value.getContentType()).isEqualTo("text/plain;charset=UTF-8");
        assertThat(value.getOriginalFilename()).isEqualTo("before_the_law.txt");
        assertThat(value.getName()).isEqualTo("Before The Law");
        assertThat(new String(value.getBytes(), StandardCharsets.UTF_8)).isEqualTo("Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law...");
    }
}
