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
import static no.difi.meldingsutveksling.nextbest.ConversationDirection.INCOMING;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageInController.class)
public class MessageInControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConversationResourceRepository repo;

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
        serviceRecord.setServiceIdentifier(DPO);
        when(sr.getServiceRecord("1")).thenReturn(serviceRecord);

        DpoConversationResource cr42 = DpoConversationResource.of("42", "2", "1");
        DpvConversationResource cr43 = DpvConversationResource.of("43", "2", "1");
        DpoConversationResource cr44 = DpoConversationResource.of("44", "1", "2");

        when(repo.findByConversationIdAndDirection("42", INCOMING)).thenReturn(Optional.of(cr42));
        when(repo.findByConversationIdAndDirection("43", INCOMING)).thenReturn(Optional.of(cr43));
        when(repo.findByConversationIdAndDirection("1337", INCOMING)).thenReturn(Optional.empty());
        when(repo.findAllByDirection(INCOMING)).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByServiceIdentifierAndDirection(DPO, INCOMING)).thenReturn(asList(cr42, cr44));
        when(repo.findByServiceIdentifierAndDirection(DPV, INCOMING)).thenReturn(asList(cr43));
        when(repo.findFirstByDirectionOrderByLastUpdateAsc(INCOMING)).thenReturn(Optional.of(cr42));
        when(repo.findFirstByServiceIdentifierAndDirectionOrderByLastUpdateAsc(DPO, INCOMING)).thenReturn(Optional.of(cr42));
    }

    @Test
    public void getMessageWithIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages")
                .accept(MediaType.APPLICATION_JSON)
                .param("conversationId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
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
                .param("serviceIdentifier", "DPO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void peekIncomingShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/peek").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
    }

    @Test
    public void peekIncomingWithMessageIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/peek")
                .accept(MediaType.APPLICATION_JSON)
                .param("serviceIdentifier", "DPO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(8)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
    }
}
