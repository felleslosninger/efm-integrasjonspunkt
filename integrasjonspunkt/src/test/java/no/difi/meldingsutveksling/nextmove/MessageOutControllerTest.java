package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Lists;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.RegexMatcher.matchesRegex;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
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
    private ConversationService conversationService;

    @MockBean
    private ConversationStrategyFactory strategyFactory;

    @MockBean
    private ServiceRegistryLookup sr;

    @MockBean
    private IntegrasjonspunktProperties props;

    @MockBean
    private MessageSender messageSender;

    @MockBean
    private NextMoveServiceBus nextMoveServiceBus;

    @MockBean
    private NextMoveUtils nextMoveUtils;

    @Before
    public void setup() {
        String filedir = "target/uploadtest/";
        IntegrasjonspunktProperties.NextBEST nextBEST = new IntegrasjonspunktProperties.NextBEST();
        nextBEST.setFiledir(filedir);
        when(nextMoveUtils.getConversationFiledirPath(Matchers.any())).thenReturn(filedir);
        IntegrasjonspunktProperties.Organization org = new IntegrasjonspunktProperties.Organization();
        org.setNumber("3");
        when(props.getNextbest()).thenReturn(nextBEST);
        when(props.getOrg()).thenReturn(org);

        IntegrasjonspunktProperties.FeatureToggle featureToggle = new IntegrasjonspunktProperties.FeatureToggle();
        featureToggle.setEnableDPO(true);
        featureToggle.setEnableDPE(true);
        when(props.getFeature()).thenReturn(featureToggle);
        when(strategyFactory.getEnabledServices()).thenReturn(Arrays.asList(DPO, DPV));

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setServiceIdentifier(DPO);
        serviceRecord.setDpeCapabilities(Lists.newArrayList());
        when(sr.getServiceRecord("1", DPO)).thenReturn(Optional.of(serviceRecord));
        when(sr.getServiceRecords("1")).thenReturn(Lists.newArrayList(serviceRecord));
        InfoRecord fooInfo = new InfoRecord("1", "foo", new EntityType("org", "org"));
        when(sr.getInfoRecord("1")).thenReturn(fooInfo);
        InfoRecord barInfo = new InfoRecord("2", "bar", new EntityType("org", "org"));
        when(sr.getInfoRecord("2")).thenReturn(barInfo);
        InfoRecord bazInfo = new InfoRecord("3", "baz", new EntityType("org", "org"));
        when(sr.getInfoRecord("3")).thenReturn(bazInfo);

        MessageStatus receiptSent = MessageStatus.of(GenericReceiptStatus.SENDT);
        MessageStatus receiptDelivered = MessageStatus.of(GenericReceiptStatus.LEVERT,
                LocalDateTime.now().plusMinutes(1));
        Conversation receiptConversation = Conversation.of("42", "42ref", "321", "123", OUTGOING, "sometitle", DPO,
                receiptDelivered, receiptSent);

        DpoConversationResource cr42 = DpoConversationResource.of("42", "2", "1");
        DpvConversationResource cr43 = DpvConversationResource.of("43", "2", "1");
        DpoConversationResource cr44 = DpoConversationResource.of("44", "1", "2");

        DpoConversationStrategy dpoMock = mock(DpoConversationStrategy.class);
        when(dpoMock.send(cr42)).thenReturn(ResponseEntity.ok().build());
        when(strategyFactory.getStrategy(cr42)).thenReturn(Optional.of(dpoMock));

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
                .andExpect(jsonPath("$.*", hasSize(10)))
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
                .andExpect(jsonPath("$.*", hasSize(10)))
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
    public void arkivmeldingShouldValidate() throws Exception {
        File arkivmelding = new File("src/test/resources/arkivmelding_ok.xml");
        byte[] am_bytes = FileUtils.readFileToByteArray(arkivmelding);
        MockMultipartFile data = new MockMultipartFile("data", "arkivmelding.xml", "application/xml", am_bytes);
        MockMultipartFile testpdf = new MockMultipartFile("data2", "test.pdf", "application/pdf", "foo".getBytes());
        mvc.perform(fileUpload("/out/messages/42")
                .file(data)
                .file(testpdf))
                .andExpect(status().isOk());
    }

    @Test
    public void arkivmeldingShouldFail() throws Exception {
        File arkivmelding = new File("src/test/resources/arkivmelding_error.xml");
        byte[] am_bytes = FileUtils.readFileToByteArray(arkivmelding);
        MockMultipartFile data = new MockMultipartFile("data", "arkivmelding.xml", "application/xml", am_bytes);
        mvc.perform(fileUpload("/out/messages/42")
                .file(data))
                .andExpect(status().is5xxServerError());
    }

    @Test
    public void getOutTypesShouldReturnDPO() throws Exception {
        mvc.perform(get("/out/types/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is("DPO")));
    }
}
