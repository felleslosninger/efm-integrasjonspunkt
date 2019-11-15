package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.Predicate;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveUploadedFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, ValidationConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(NextMoveMessageOutController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
public class NextMoveMessageOutControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NextMoveMessageService messageService;
    @MockBean private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock private NextMoveOutMessage messageMock;
    @Mock private IntegrasjonspunktProperties.Organization organization;

    @Captor private ArgumentCaptor<NextMoveUploadedFile> nextMoveUploadedFileArgumentCaptor;

    private MessageData arkivmeldingMessageData = new MessageData()
            .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
            .setType("arkivmelding")
            .setBusinessMessage(new ArkivmeldingMessage()
                    .setHoveddokument("before_the_law.txt"));

    private NextMoveOutMessage arkivmeldingMessage = NextMoveOutMessage.of(getResponseSbd(arkivmeldingMessageData), ServiceIdentifier.DPO);

    private MessageData dpiDigitalMessageData = new MessageData()
            .setStandard("urn:no:difi:digitalpost:xsd:digital::digital")
            .setType("digital")
            .setBusinessMessage(new DpiDigitalMessage()
                    .setSikkerhetsnivaa(4)
                    .setHoveddokument("kafka_quotes.txt")
                    .setSpraak("en")
                    .setTittel("Kafka quotes")
                    .setDigitalPostInfo(new DigitalPostInfo()
                            .setVirkningsdato(LocalDate.parse("2019-04-01"))
                            .setAapningskvittering(true)
                    ).setVarsler(new DpiNotification()
                            .setEpostTekst("Many a book is like a key to unknown chambers within the castle of one’s own self.")
                            .setSmsTekst("A book must be the axe for the frozen sea within us.")
                    )

            );

    private NextMoveOutMessage dpiDigitalMessage = NextMoveOutMessage.of(getResponseSbd(dpiDigitalMessageData), ServiceIdentifier.DPO);

    @Data
    private static class MessageData {
        private final String messageId = UUID.randomUUID().toString();
        private final String conversationId = UUID.randomUUID().toString();
        private BusinessMessage businessMessage;
        private String standard;
        private String type;
    }

    @Before
    public void before() {
        given(organization.getNumber()).willReturn("910077473");
        given(integrasjonspunktProperties.getOrg()).willReturn(organization);
    }

    @Test
    public void multipart() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class), anyList())).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(arkivmeldingMessage.getSbd());

        mvc.perform(
                MockMvcRequestBuilders.multipart("/api/messages/out/multipart")
                        .file(new MockMultipartFile("sbd", null, MediaType.APPLICATION_JSON_UTF8_VALUE, objectMapper.writeValueAsBytes(getInputSbd(arkivmeldingMessageData))))
                        .file(new MockMultipartFile("Before The Law", "before_the_law.txt", MediaType.TEXT_PLAIN_VALUE, "Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law...".getBytes(StandardCharsets.UTF_8)))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                                        .description("There can be zero or more attachments. The name of the part will be used as a title for the attachment.\n" +
                                                "The originalFilename will be used as the name of the file in the ASiC.\n" +
                                                "Please note that the Content-Type of the part must be set to the MIME-type of the attachment.")
                        ),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class), anyList());
    }

    @Test
    public void createArkivmeldingMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(arkivmeldingMessage.getSbd());

        mvc.perform(
                post("/api/messages/out")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(getInputSbd(arkivmeldingMessageData)))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
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
    }

    @Test
    public void createDpiDigitalMessage() throws Exception {
        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(dpiDigitalMessage.getSbd());

        mvc.perform(
                post("/api/messages/out")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(getInputSbd(dpiDigitalMessageData)))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
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
    }

    @Test
    public void find() throws Exception {
        given(messageService.findMessages(any(Predicate.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<NextMoveMessage> content = Arrays.asList(arkivmeldingMessage, dpiDigitalMessage);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/messages/out")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/find",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("conversationId").optional().description("Filter on conversationId"),
                                parameterWithName("messageId").optional().description("Filter on messageId"),
                                parameterWithName("processIdentifier").optional().description("Filter on processIdentifier"),
                                parameterWithName("receiverIdentifier").optional().description("Filter on receiverIdentifier"),
                                parameterWithName("senderIdentifier").optional().description("Filter on senderIdentifier"),
                                parameterWithName("serviceIdentifier").optional().description(String.format("Filter on service identifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("content[].standardBusinessDocumentHeader."))
                                .and(subsectionWithPath("content[].arkivmelding").description("The DPO business message").optional())
                                .and(arkivmeldingMessageDescriptors("content[].arkivmelding."))
                                .and(subsectionWithPath("content[].digital.").description("The digital DPI business message").optional())
                                .and(dpiDigitalMessageDescriptors("content[].digital."))
                                .and(getPageFieldDescriptors())
                                .andWithPrefix("pageable.", getPageableFieldDescriptors())
                        )
                );

        verify(messageService).findMessages(any(Predicate.class), any(Pageable.class));
    }

    @Test
    public void getMessage() throws Exception {
        given(messageService.getMessage(any())).willReturn(arkivmeldingMessage);

        mvc.perform(
                get("/api/messages/out/{messageId}", arkivmeldingMessage.getMessageId())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("The messageId")
                        ),
                        responseFields()
                                .and(standardBusinessDocumentHeaderDescriptors("standardBusinessDocumentHeader."))
                                .and(subsectionWithPath("arkivmelding").description("The DPO business message"))
                                .and(arkivmeldingMessageDescriptors("arkivmelding."))
                        )
                );

        verify(messageService).getMessage(eq(arkivmeldingMessageData.messageId));
    }

    @Test
    public void sendMessage() throws Exception {
        given(messageService.getMessage(any())).willReturn(arkivmeldingMessage);

        mvc.perform(
                post("/api/messages/out/{messageId}", arkivmeldingMessage.getMessageId())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/send",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("The messageId")
                        )
                        )
                );

        verify(messageService).getMessage(eq(arkivmeldingMessage.getMessageId()));
        verify(messageService).sendMessage(same(arkivmeldingMessage));
    }

    @Test
    public void deleteMessage() throws Exception {
        mvc.perform(
                delete("/api/messages/out/{messageId}", arkivmeldingMessage.getMessageId())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("messages/out/delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("The messageId")
                        )
                        )
                );

        verify(messageService).deleteMessage(eq(arkivmeldingMessageData.messageId));
    }

    @Test
    public void uploadFile() throws Exception {
        given(messageService.getMessage(any())).willReturn(arkivmeldingMessage);

        mvc.perform(
                put("/api/messages/out/{messageId}", arkivmeldingMessageData.messageId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; name=Before The Law; filename=before_the_law.txt")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                        requestParameters(
                                parameterWithName("messageId").optional().description("The messageId"),
                                parameterWithName("title").optional().description("The attachment title can alternatively be specified in this request parameter. " +
                                        "If not specified here, then the title is extracted from the Content-Disposition HTTP header.")
                        ))
                );

        verify(messageService).getMessage(eq(arkivmeldingMessageData.messageId));
        verify(messageService).addFile(same(arkivmeldingMessage), nextMoveUploadedFileArgumentCaptor.capture());

        NextMoveUploadedFile value = nextMoveUploadedFileArgumentCaptor.getValue();
        assertThat(value.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
        assertThat(value.getOriginalFilename()).isEqualTo("before_the_law.txt");
        assertThat(value.getName()).isEqualTo("Before The Law");
        assertThat(new String(value.getBytes(), StandardCharsets.UTF_8)).isEqualTo("Before the law sits a gatekeeper. To this gatekeeper comes a man from the country who asks to gain entry into the law...");
    }


//    @PostMapping("/{messageId}")
//    @ApiOperation(value = "Send message", notes = "Send the message with supplied messageId")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "Success", response = StandardBusinessDocument.class),
//            @ApiResponse(code = 400, message = "Bad request", response = String.class)
//    })
//    public void sendMessage(
//            @ApiParam(
//                    value = "The message ID. Usually a UUID",
//                    example = "90c0bacf-c233-4a54-96fc-e205b79862d9",
//                    required = true
//            )
//            @PathVariable("messageId") String messageId) {
//        NextMoveOutMessage message = messageService.getMessage(messageId);
//        messageService.sendMessage(message);
//    }

    private StandardBusinessDocument getInputSbd(MessageData message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        return sbd;
    }

    private StandardBusinessDocument getResponseSbd(MessageData message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        sbd.getStandardBusinessDocumentHeader().getDocumentIdentification()
                .setCreationDateAndTime(OffsetDateTime.parse("2019-03-25T11:38:23+02:00"));

        return sbd;
    }

    private void fill(StandardBusinessDocument sbd, MessageData message) {
        sbd.setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setBusinessScope(new BusinessScope()
                        .addScope(new Scope()
                                .addScopeInformation(new CorrelationInformation()
                                        .setExpectedResponseDateTime(OffsetDateTime.parse("2019-04-25T11:38:23+02:00"))
                                )
                                .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                .setInstanceIdentifier(message.conversationId)
                                .setType("ConversationId")
                        )
                )
                .setDocumentIdentification(new DocumentIdentification()
                        .setInstanceIdentifier(message.messageId)
                        .setStandard(message.getStandard())
                        .setType(message.getType())
                        .setTypeVersion("1.0")
                )
                .setHeaderVersion("1.0")
                .addReceiver(new Receiver()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority("iso6523-actorid-upis")
                                .setValue("0192:910075918")
                        )
                )
                .addSender(new Sender()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority("iso6523-actorid-upis")
                                .setValue("0192:910077473")
                        )
                )
        )
                .setAny(message.businessMessage);
    }
}