package no.difi.meldingsutveksling.nextbest;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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

    @Before
    public void setup() {
        IntegrasjonspunktProperties.NextBEST nextBEST = new IntegrasjonspunktProperties.NextBEST();
        nextBEST.setFiledir("target/uploadtest");
        when(props.getNextbest()).thenReturn(nextBEST);

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setServiceIdentifier(ServiceIdentifier.EDU.name());
        when(sr.getServiceRecord("1")).thenReturn(serviceRecord);

        OutgoingConversationResource cr42 = OutgoingConversationResource.of("42", "1", "1");
        OutgoingConversationResource cr43 = OutgoingConversationResource.of("43", "1", "2");
        OutgoingConversationResource cr44 = OutgoingConversationResource.of("44", "2", "1");

        when(repo.findOne("42")).thenReturn(cr42);
        when(repo.findOne("43")).thenReturn(cr43);
        when(repo.findOne("1337")).thenReturn(null);
        when(repo.findAll()).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByReceiverId("1")).thenReturn(asList(cr42, cr43));
        when(repo.findByReceiverId("2")).thenReturn(asList(cr44));
        when(repo.findByMessagetypeId("1")).thenReturn(asList(cr42, cr44));
        when(repo.findByMessagetypeId("2")).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndMessagetypeId("1", "1")).thenReturn(asList(cr42));
        when(repo.findByReceiverIdAndMessagetypeId("1", "2")).thenReturn(asList(cr43));
        when(repo.findByReceiverIdAndMessagetypeId("2", "1")).thenReturn(asList(cr44));
    }

    @Test
    public void getCapabilitiesShouldReturnOk() throws Exception {
        mvc.perform(get("/receivers/1/capabilities").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("['EDU']", true));
    }

    @Test
    public void getMessageShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")))
                .andExpect(jsonPath("$.fileRefs", hasSize(0)));
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
                .andExpect(jsonPath("$[*].messagetypeId", containsInAnyOrder("1", "2")))
                .andExpect(jsonPath("$[*].fileRefs", containsInAnyOrder(hasSize(0), hasSize(0))));
    }

    @Test
    public void getMessagesWithTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("messagetypeId", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].conversationId", containsInAnyOrder("42", "44")))
                .andExpect(jsonPath("$[*].receiverId", containsInAnyOrder("1", "2")))
                .andExpect(jsonPath("$[*].messagetypeId", containsInAnyOrder("1", "1")))
                .andExpect(jsonPath("$[*].fileRefs", containsInAnyOrder(hasSize(0), hasSize(0))));
    }

    @Test
    public void getMessagesWithReceiverAndTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/out/messages")
                .param("receiverId", "1")
                .param("messagetypeId", "2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].conversationId", is("43")))
                .andExpect(jsonPath("$[0].receiverId", is("1")))
                .andExpect(jsonPath("$[0].messagetypeId", is("2")))
                .andExpect(jsonPath("$[0].fileRefs", hasSize(0)));
    }

    @Test
    public void createResourceWithConversationIdShouldReturnExisting() throws Exception {
        mvc.perform(post("/out/messages")
                .param("receiverId", "1")
                .param("messagetypeId", "1")
                .param("conversationId", "42")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")))
                .andExpect(jsonPath("$.fileRefs", hasSize(0)));
    }

    @Test
    public void createResourceWithoutReceiverShouldFail() throws Exception {
        mvc.perform(post("/out/messages")
                .param("messagetypeId", "1")
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
                .param("messagetypeId", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", matchesRegex("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")));
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
                .andExpect(jsonPath("$.fileRefs", hasSize(1)))
                .andExpect(jsonPath("$.fileRefs[0]", is("file.txt")));
    }
}
