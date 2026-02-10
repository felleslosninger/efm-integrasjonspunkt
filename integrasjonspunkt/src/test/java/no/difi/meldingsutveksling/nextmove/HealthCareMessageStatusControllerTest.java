package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.nhn.ApplicationReceiptError;
import no.difi.meldingsutveksling.nextmove.nhn.FeilmeldingForApplikasjonskvittering;
import no.difi.meldingsutveksling.nextmove.nhn.IncomingReceipt;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nextmove.nhn.StatusForMottakAvMelding;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusQueryInput;
import no.difi.meldingsutveksling.status.MessageStatusRepository;
import no.difi.meldingsutveksling.status.service.MessageStatusController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.meldingsutveksling.nextmove.MessageStatusTestData.messageStatus1;
import static no.difi.meldingsutveksling.nextmove.MessageStatusTestData.messageStatus2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest({MessageStatusController.class})
@WithMockUser
@ActiveProfiles("test")
public class HealthCareMessageStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageStatusRepository statusRepo;

    @MockitoBean
    private StatusQueue statusQueue;

    @MockitoBean
    private NhnAdapterClient nhnAdapterClient;

    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    public void setup(){

        webClient = MockMvcWebTestClient.bindTo(mockMvc).codecs(t -> {
            t.defaultCodecs()
                .jackson2JsonDecoder(new Jackson2JsonDecoder(ObjectMapperHolder.get()));
            t.defaultCodecs()
                .jackson2JsonEncoder(new Jackson2JsonEncoder(ObjectMapperHolder.get()));
        }).build();

    }

    @Test
    public void whenProcesIsNotHealthcare_nhnAdapterIsNotinvoked() {
        var messageStatus1 = messageStatus1();
        var messageStatus2 = messageStatus2();
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
            .willAnswer(invocation -> {
                List<MessageStatus> content = Arrays.asList(messageStatus1, messageStatus2);
                return new PageImpl<>(content, invocation.getArgument(1), content.size());
            });
        var response = webClient.get()
            .uri("/api/statuses")
            .accept(MediaType.APPLICATION_JSON)
            .exchange();
        response.expectStatus().isOk().expectBody()
            .jsonPath("$.content[0].id").isEqualTo(messageStatus1.getId())
            .jsonPath("$.content[1].id").isEqualTo(messageStatus2.getId());

        verifyNoInteractions(nhnAdapterClient);

    }


    @Test
    public void whenHealthcareStatus_and_status_is_SENDT_MOTTATT_LEST_then_apprecIsNotretrieved() {

        Map<ReceiptStatus,String> statuses = Map.of(ReceiptStatus.SENDT,MessageStatusController.FASTLEGE_PROCESS,ReceiptStatus.MOTTATT,MessageStatusController.NHN_PROCESS,ReceiptStatus.LEST,MessageStatusController.FASTLEGE_PROCESS,ReceiptStatus.LEVERT,MessageStatusController.FASTLEGE_PROCESS);
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
            .willAnswer(invocation -> {
                List<MessageStatus> content = statuses.entrySet().stream().map(entry -> healthCareStatus(entry.getKey(),entry.getKey().name() ,entry.getValue())).toList();
                return new PageImpl<>(content, invocation.getArgument(1), content.size());
            });

        var response = webClient.get().uri("/api/statuses").accept(MediaType.APPLICATION_JSON).exchange();

        response.expectStatus().isOk().expectBody().jsonPath("$.content.length()").isEqualTo(statuses.size());
        verifyNoInteractions(nhnAdapterClient);
    }

    @Test
    public void whenHealthcareStatusFAILED_apprecIsRetrieved() throws EncryptionException {
        List<MessageStatus> failedMessages = List.of(healthCareStatus(ReceiptStatus.FEIL,"Failed during transport",MessageStatusController.FASTLEGE_PROCESS),healthCareStatus(ReceiptStatus.FEIL,"Person not found",MessageStatusController.NHN_PROCESS));
        List<MessageStatus> okMessage = List.of(messageStatus1());
        List<MessageStatus> allMessages = Stream.concat(failedMessages.stream(),okMessage.stream()).collect(Collectors.toList());
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
            .willAnswer(invocation -> new PageImpl<>(allMessages, invocation.getArgument(1), allMessages.size()));
        given(nhnAdapterClient.messageReceipt(any(),any())).willReturn(

            List.of(new IncomingReceipt("1111", StatusForMottakAvMelding.AVVIST,List.of(new ApplicationReceiptError(FeilmeldingForApplikasjonskvittering.LEGE_FINNES_IKKE,"some details"))))
            );
        var response = webClient.get().uri("/api/statuses").accept(MediaType.APPLICATION_JSON).exchange();

        response.expectStatus().isOk().expectBody().jsonPath("$.content.length()").isEqualTo(allMessages.size())
            .jsonPath("$.content[0].status").isEqualTo(ReceiptStatus.FEIL)
            .jsonPath("$.content[1].status").isEqualTo(ReceiptStatus.FEIL)
            .jsonPath("$.content[2].status").isEqualTo(ReceiptStatus.MOTTATT)
            .jsonPath("$.content[0].rawReceipt").value(this::validateReceipt)
                .jsonPath("$.content[1].rawReceipt").value(this::validateReceipt
            ).jsonPath("$.content[2].rawReceipt").doesNotExist();

        verify(nhnAdapterClient, times(2)).messageReceipt(any(),any());



    }

    private  void validateReceipt(Object receipt) {
        try {
            IncomingReceipt reciept = ObjectMapperHolder.get().readValue((String) receipt, IncomingReceipt.class);
            assertEquals(reciept.status(),StatusForMottakAvMelding.AVVIST);
            assertEquals(reciept.errors().getFirst().type(),FeilmeldingForApplikasjonskvittering.LEGE_FINNES_IKKE);
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }


        static MessageStatus healthCareStatus(ReceiptStatus receiptStatus,String description,String process) {
           return healthCareStatus(receiptStatus,description,process,"");
        }

    static MessageStatus healthCareStatus(ReceiptStatus receiptStatus,String description,String process,String rawReceipt) {
        MessageStatus messageStatus = new MessageStatus()
            .setStatus(receiptStatus.toString())
            .setLastUpdate(OffsetDateTime.parse("2019-11-05T12:04:34+02:00"))
            .setDescription(description)
            .setRawReceipt(rawReceipt);
        setIdentifier(messageStatus);
        Conversation conversation = new Conversation().setMessageId(UUID.randomUUID().toString())
            .setProcessIdentifier(process)
            .setMessageReference(UUID.randomUUID().toString())
            .setConversationId(UUID.randomUUID().toString())
            .setMessageStatuses(Set.of(messageStatus));
        messageStatus.setConversation(conversation);

        return messageStatus;
    }

        private static void setIdentifier(MessageStatus messageStatus) {
            try {
                Field idField = AbstractEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(messageStatus, new Random().nextLong());

            }catch (Exception e) {
                fail("Not able to set identiffier on MessageStatus");
            }
        }
    }

