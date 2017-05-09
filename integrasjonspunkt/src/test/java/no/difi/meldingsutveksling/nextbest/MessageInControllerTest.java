package no.difi.meldingsutveksling.nextbest;

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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageInController.class)
public class MessageInControllerTest {

    @Autowired
    private MockMvc mvc;

    private DirectionalConversationResourceRepository repo;

    @MockBean
    private ServiceRegistryLookup sr;

    @MockBean
    private IntegrasjonspunktProperties props;

    @Before
    public void setup() {
        repo = mock(DirectionalConversationResourceRepository.class);
        IntegrasjonspunktProperties.NextBEST nextBEST = new IntegrasjonspunktProperties.NextBEST();
        nextBEST.setFiledir("target/uploadtest");
        when(props.getNextbest()).thenReturn(nextBEST);

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setServiceIdentifier(DPO);
        when(sr.getServiceRecord("1")).thenReturn(serviceRecord);

        DpoConversationResource cr42 = DpoConversationResource.of("42", "2", "1");
        DpoConversationResource cr43 = DpoConversationResource.of("43", "2", "1");
        DpoConversationResource cr44 = DpoConversationResource.of("44", "1", "2");

        when(repo.findByConversationId("42")).thenReturn(Optional.of(cr42));
        when(repo.findByConversationId("43")).thenReturn(Optional.of(cr43));
        when(repo.findByConversationId("1337")).thenReturn(Optional.empty());
        when(repo.findAll()).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByServiceIdentifier(DPO)).thenReturn(asList(cr42, cr44));
        when(repo.findByServiceIdentifier(DPV)).thenReturn(asList(cr43));
        when(repo.findFirstByOrderByLastUpdateAsc()).thenReturn(Optional.of(cr42));
        when(repo.findFirstByServiceIdentifierOrderByLastUpdateAsc(DPO)).thenReturn(Optional.of(cr42));
    }

    @Test
    public void getMessageWithIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages")
                .accept(MediaType.APPLICATION_JSON)
                .param("conversationId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")));
    }

    @Test
    public void getMessageWithUnknownIdShouldFail() throws Exception {
        mvc.perform(get("/in/messages/1337").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getAllMessageShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    public void getMessagesWithTypeShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages").accept(MediaType.APPLICATION_JSON)
                .param("messagetypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void peekIncomingShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/peek").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")));
    }

    @Test
    public void peekIncomingWithMessageIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/peek")
                .accept(MediaType.APPLICATION_JSON)
                .param("messagetypeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(5)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")));
    }
}
