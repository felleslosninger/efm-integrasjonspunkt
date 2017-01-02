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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Arrays.asList;
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
    private IncomingConversationResourceRepository repo;

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

        IncomingConversationResource cr42 = IncomingConversationResource.of("42", "1", "1");
        IncomingConversationResource cr43 = IncomingConversationResource.of("43", "1", "2");
        IncomingConversationResource cr44 = IncomingConversationResource.of("44", "2", "1");

        when(repo.findOne("42")).thenReturn(cr42);
        when(repo.findOne("43")).thenReturn(cr43);
        when(repo.findOne("1337")).thenReturn(null);
        when(repo.findAll()).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByMessagetypeId("1")).thenReturn(asList(cr42, cr44));
        when(repo.findByMessagetypeId("2")).thenReturn(asList(cr43));
    }

    @Test
    public void getMessageWithIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(4)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.messagetypeId", is("1")))
                .andExpect(jsonPath("$.fileRefs", hasSize(0)));
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
    public void popIncomingShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/pop/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
