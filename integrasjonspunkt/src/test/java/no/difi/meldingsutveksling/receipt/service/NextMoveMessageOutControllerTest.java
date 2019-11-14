package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.config.ValidationConfig;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
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

    @Data
    public static class Message {
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
        Message message = new Message()
                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
                .setType("arkivmelding")
                .setBusinessMessage(new ArkivmeldingMessage()
                        .setHoveddokument("before_the_law.txt"));


        given(messageService.createMessage(any(StandardBusinessDocument.class), anyList())).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(getResponseSbd(message));

        mvc.perform(
                MockMvcRequestBuilders.multipart("/api/messages/out/multipart")
                        .file(new MockMultipartFile("sbd", null, MediaType.APPLICATION_JSON_UTF8_VALUE, objectMapper.writeValueAsBytes(getInputSbd(message))))
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
                        responseFieldsStandardBusinessDocument()
                                .andWithPrefix("arkivmelding.",
                                        fieldWithPath("hoveddokument")
                                                .type(JsonFieldType.STRING)
                                                .optional()
                                                .description("Name of the attachment that is the main document.")
                                )
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class), anyList());
    }

    @Test
    public void createMessage() throws Exception {
        ConstrainedFields fields = new ConstrainedFields(StandardBusinessDocument.class);

        Message message = new Message()
                .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
                .setType("arkivmelding")
                .setBusinessMessage(new ArkivmeldingMessage()
                        .setHoveddokument("before_the_law.txt"));


        given(messageService.createMessage(any(StandardBusinessDocument.class))).willReturn(messageMock);
        given(messageMock.getSbd()).willReturn(getResponseSbd(message));

        mvc.perform(
                post("/api/messages/out")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsBytes(getInputSbd(message)))
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
                        requestFieldsStandardBusinessDocument()
                                .andWithPrefix("arkivmelding.",
                                        fieldWithPath("hoveddokument")
                                                .type(JsonFieldType.STRING)
                                                .optional()
                                                .description("Name of the attachment that is the main document.")
                                ),
                        responseFieldsStandardBusinessDocument()
                                .andWithPrefix("arkivmelding.",
                                        fieldWithPath("hoveddokument")
                                                .type(JsonFieldType.STRING)
                                                .optional()
                                                .description("Name of the attachment that is the main document.")
                                )
                        )
                );

        verify(messageService).createMessage(any(StandardBusinessDocument.class));
    }


    private StandardBusinessDocument getInputSbd(Message message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        return sbd;
    }

    private StandardBusinessDocument getResponseSbd(Message message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        sbd.getStandardBusinessDocumentHeader().getDocumentIdentification()
                .setCreationDateAndTime(OffsetDateTime.parse("2019-03-25T11:38:23+02:00"));

        return sbd;
    }

    private void fill(StandardBusinessDocument sbd, Message message) {
        sbd.setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setBusinessScope(new BusinessScope()
                        .addScope(new Scope()
                                .addScopeInformation(new CorrelationInformation()
                                        .setExpectedResponseDateTime(OffsetDateTime.parse("2019-04-25T11:38:23+02:00"))
                                )
                                .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                .setType("ConversationId")
                        )
                )
                .setDocumentIdentification(new DocumentIdentification()
                        .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
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