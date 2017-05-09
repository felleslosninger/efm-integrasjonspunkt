package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.RegexMatcher.matchesRegex;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextbest.ConversationDirection.OUTGOING;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageOutController.class)
public class MessageOutControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConversationResourceRepository repo;

    @MockBean
    private ServiceRegistryLookup sr;

    @MockBean
    private IntegrasjonspunktProperties props;

    @MockBean
    private ConversationRepository crepo;

    @MockBean
    private MessageSender messageSender;

    @MockBean
    private NextBestServiceBus nextBestServiceBus;

    @Before
    public void setup() {
        IntegrasjonspunktProperties.NextBEST nextBEST = new IntegrasjonspunktProperties.NextBEST();
        nextBEST.setFiledir("target/uploadtest");
        IntegrasjonspunktProperties.Organization org = new IntegrasjonspunktProperties.Organization();
        org.setNumber("3");
        when(props.getNextbest()).thenReturn(nextBEST);
        when(props.getOrg()).thenReturn(org);

        IntegrasjonspunktProperties.FeatureToggle featureToggle = new IntegrasjonspunktProperties.FeatureToggle();
        featureToggle.setEnableDPO(true);
        featureToggle.setEnableDPE(true);
        when(props.getFeature()).thenReturn(featureToggle);

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setServiceIdentifier(DPO);
        serviceRecord.setDpeCapabilities(Lists.newArrayList());
        when(sr.getServiceRecord("1")).thenReturn(serviceRecord);

        MessageStatus receiptSent = MessageStatus.of(GenericReceiptStatus.SENDT.toString(), LocalDateTime.now());
        MessageStatus receiptDelivered = MessageStatus.of(GenericReceiptStatus.LEVERT.toString(),
                LocalDateTime.now().plusMinutes(1));
        Conversation receiptConversation = Conversation.of("42", "42ref", "123", "sometitle", DPO,
                receiptDelivered, receiptSent);
        when(crepo.findByConversationId("42")).thenReturn(asList(receiptConversation));

        DpoConversationResource cr42 = DpoConversationResource.of("42", "2", "1");
        DpvConversationResource cr43 = DpvConversationResource.of("43", "2", "1");
        DpoConversationResource cr44 = DpoConversationResource.of("44", "1", "2");

        when(repo.findByConversationIdAndDirection("42", OUTGOING)).thenReturn(Optional.of(cr42));
        when(repo.findByConversationIdAndDirection("43", OUTGOING)).thenReturn(Optional.of(cr43));
        when(repo.findByConversationIdAndDirection("1337", OUTGOING)).thenReturn(Optional.empty());
        when(repo.findAll()).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByReceiverIdAndDirection("1", OUTGOING)).thenReturn(asList(cr42, cr43));
        when(repo.findByReceiverIdAndDirection("2", OUTGOING)).thenReturn(asList(cr44));
        when(repo.findByServiceIdentifierAndDirection(DPO, OUTGOING)).thenReturn(asList(cr42, cr44));
        when(repo.findByServiceIdentifierAndDirection(DPV, OUTGOING)).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndServiceIdentifierAndDirection("1", DPO, OUTGOING)).thenReturn(asList(cr42));
        when(repo.findByReceiverIdAndServiceIdentifierAndDirection("1", DPV, OUTGOING)).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndServiceIdentifierAndDirection("2", DPO, OUTGOING)).thenReturn(asList(cr44));
    }

    @Test
    public void getMessageShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")))
                .andExpect(jsonPath("$.fileRefs.*", hasSize(0)));
    }

    @Test
    public void getMessageShouldReturnNotFound() throws Exception {
        mvc.perform(get("/out/messages/1337")).andExpect(status().isNotFound());
    }

    @Test
    public void getMessagesWithReceiversShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("receiverId", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].conversationId", containsInAnyOrder("42", "43")))
                .andExpect(jsonPath("$[*].receiverId", containsInAnyOrder("1", "1")))
                .andExpect(jsonPath("$[*].serviceIdentifier", containsInAnyOrder("DPO", "DPV")));
    }

    @Test
    public void getMessagesWithTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("serviceIdentifier", "DPO")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].conversationId", containsInAnyOrder("42", "44")))
                .andExpect(jsonPath("$[*].receiverId", containsInAnyOrder("1", "2")))
                .andExpect(jsonPath("$[*].serviceIdentifier", containsInAnyOrder("DPO", "DPO")));
    }

    @Test
    public void getMessagesWithReceiverAndTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("receiverId", "1")
                .param("serviceIdentifier", "DPV")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId", is("43")))
                .andExpect(jsonPath("$[0].receiverId", is("1")))
                .andExpect(jsonPath("$[0].serviceIdentifier", is("DPV")))
                .andExpect(jsonPath("$[0].fileRefs.*", hasSize(0)));
    }

    @Test
    public void createResourceWithConversationIdShouldReturnExisting() throws Exception {
        mvc.perform(post("/out/messages")
                .content("{\"receiverId\": 1, \"serviceIdentifier\": \"DPO\", \"conversationId\": \"42\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")))
                .andExpect(jsonPath("$.fileRefs.*", hasSize(0)));
    }

    @Test
    public void createResourceWithoutReceiverShouldFail() throws Exception {
        mvc.perform(post("/out/messages")
                .param("serviceIdentifier", "DPO")
                .param("conversationId", "42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createResourceWithoutTypeShouldFail() throws Exception {
        mvc.perform(post("/out/messages")
                .param("receiverId", "1")
                .param("conversationId", "42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createResourceWithoutConversationIdShouldReturnOk() throws Exception {
        mvc.perform(post("/out/messages")
                .content("{ \"receiverId\": 1, \"serviceIdentifier\": \"DPO\" }")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", matchesRegex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.senderId", is("3")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
    }

    @Test
    public void testFileUpload() throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "file.txt", "text/plain", "some text".getBytes());
        mvc.perform(fileUpload("/out/messages/42")
                .file(data))
                .andExpect(status().isOk());

        // Check that file is added to the conversation resource
        mvc.perform(get("/out/messages/42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileRefs.*", hasSize(1)))
                .andExpect(jsonPath("$.fileRefs.0", is("file.txt")));
    }

    @Test
    public void getOutTypesShouldReturnDPO() throws Exception {
        mvc.perform(get("/out/types/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is("DPO")));
    }
}
