package no.difi.meldingsutveksling.nextbest;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.MessageReceipt;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
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

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.RegexMatcher.matchesRegex;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageOutController.class)
public class MessageOutControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OutgoingConversationResourceRepository repo;

    @MockBean
    private IncomingConversationResourceRepository increpo;

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
        serviceRecord.setServiceIdentifier(ServiceIdentifier.DPO);
        serviceRecord.setDpeCapabilities(Lists.newArrayList());
        when(sr.getServiceRecord("1")).thenReturn(serviceRecord);

        MessageReceipt receiptSent = MessageReceipt.of(ReceiptStatus.SENT, LocalDateTime.now());
        MessageReceipt receiptDelivered = MessageReceipt.of(ReceiptStatus.DELIVERED, LocalDateTime.now().plusMinutes(1));
        Conversation receiptConversation = Conversation.of("42", "42ref", "123", "sometitle", ServiceIdentifier.DPO,
                receiptDelivered, receiptSent);
        when(crepo.findByConversationId("42")).thenReturn(asList(receiptConversation));

        OutgoingConversationResource cr42 = OutgoingConversationResource.of("42", "2", "1", "DPO");
        OutgoingConversationResource cr43 = OutgoingConversationResource.of("43", "2", "1", "DPV");
        OutgoingConversationResource cr44 = OutgoingConversationResource.of("44", "1", "2", "DPO");

        when(repo.findOne("42")).thenReturn(cr42);
        when(repo.findOne("43")).thenReturn(cr43);
        when(repo.findOne("1337")).thenReturn(null);
        when(repo.findAll()).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByReceiverId("1")).thenReturn(asList(cr42, cr43));
        when(repo.findByReceiverId("2")).thenReturn(asList(cr44));
        when(repo.findByMessagetypeId("DPO")).thenReturn(asList(cr42, cr44));
        when(repo.findByMessagetypeId("DPV")).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndMessagetypeId("1", "DPO")).thenReturn(asList(cr42));
        when(repo.findByReceiverIdAndMessagetypeId("1", "DPV")).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndMessagetypeId("2", "DPO")).thenReturn(asList(cr44));
    }

    @Test
    public void getTracingsForIdShouldReturnOk() throws Exception {
        mvc.perform(get("/tracings/42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageReceipts", hasSize(2)));
    }

    @Test
    public void getTracingWithOnlylastShouldReturnOk() throws Exception {
        mvc.perform(get("/tracings/42")
                .param("lastonly", "true")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DELIVERED")));
    }

    @Test
    public void getTracingsForUnknownIdShouldFail() throws Exception {
        mvc.perform(get("/tracings/43")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getMessageShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("DPO")))
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
                .andExpect(jsonPath("$[*].messagetypeId", containsInAnyOrder("DPO", "DPV")));
    }

    @Test
    public void getMessagesWithTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("messagetypeId", "DPO")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].conversationId", containsInAnyOrder("42", "44")))
                .andExpect(jsonPath("$[*].receiverId", containsInAnyOrder("1", "2")))
                .andExpect(jsonPath("$[*].messagetypeId", containsInAnyOrder("DPO", "DPO")));
    }

    @Test
    public void getMessagesWithReceiverAndTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("receiverId", "1")
                .param("messagetypeId", "DPV")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId", is("43")))
                .andExpect(jsonPath("$[0].receiverId", is("1")))
                .andExpect(jsonPath("$[0].messagetypeId", is("DPV")))
                .andExpect(jsonPath("$[0].fileRefs.*", hasSize(0)));
    }

    @Test
    public void createResourceWithConversationIdShouldReturnExisting() throws Exception {
        mvc.perform(post("/out/messages")
                .param("receiverId", "1")
                .param("messagetypeId", "DPO")
                .param("conversationId", "42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("DPO")))
                .andExpect(jsonPath("$.fileRefs.*", hasSize(0)));
    }

    @Test
    public void createResourceWithoutReceiverShouldFail() throws Exception {
        mvc.perform(post("/out/messages")
                .param("messagetypeId", "DPO")
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
                .param("receiverId", "1")
                .param("messagetypeId", "DPO")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", matchesRegex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.senderId", is("3")))
                .andExpect(jsonPath("$.messagetypeId", is("DPO")));
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
