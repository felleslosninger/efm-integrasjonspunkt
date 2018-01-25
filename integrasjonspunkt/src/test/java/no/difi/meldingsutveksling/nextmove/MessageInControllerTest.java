package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(MessageInController.class)
public class MessageInControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConversationResourceRepository repo;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private ServiceRegistryLookup sr;

    @MockBean
    private IntegrasjonspunktProperties props;

    @MockBean
    private NextMoveUtils nextMoveUtils;

    @Before
    public void setup() throws IOException {
        String filedir = "target/uploadtest/";
        IntegrasjonspunktProperties.NextBEST nextBEST = new IntegrasjonspunktProperties.NextBEST();
        nextBEST.setFiledir(filedir);
        when(props.getNextbest()).thenReturn(nextBEST);
        when(nextMoveUtils.getConversationFiledirPath(any())).thenReturn(filedir);

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setServiceIdentifier(DPO);
        when(sr.getServiceRecord("1", DPO)).thenReturn(Optional.of(serviceRecord));

        DpoConversationResource cr42 = DpoConversationResource.of("42", Sender.of("2", "bar"), Receiver.of("1", "foo"));
        DpvConversationResource cr43 = DpvConversationResource.of("43", Sender.of("2", "bar"), Receiver.of("1", "foo"));
        DpoConversationResource cr44 = DpoConversationResource.of("44", Sender.of("1", "foo"), Receiver.of("2", "bar"));

        File foo = new File("src/test/resources/testfil.txt");
        File targetFoo = new File("target/uploadtest/testfil.txt");
        targetFoo.getParentFile().mkdirs();
        FileUtils.copyFile(foo, targetFoo);
        cr42.addFileRef("testfil.txt");

        when(repo.findByConversationIdAndDirection("42", INCOMING)).thenReturn(Optional.of(cr42));
        when(repo.findByConversationIdAndDirection("43", INCOMING)).thenReturn(Optional.of(cr43));
        when(repo.findByConversationIdAndDirection("1337", INCOMING)).thenReturn(Optional.empty());
        when(repo.findAllByDirection(INCOMING)).thenReturn(asList(cr42, cr43, cr44));
        when(repo.findByServiceIdentifierAndDirection(DPO, INCOMING)).thenReturn(asList(cr42, cr44));
        when(repo.findByServiceIdentifierAndDirection(DPV, INCOMING)).thenReturn(asList(cr43));
        when(repo.findFirstByDirectionOrderByLastUpdateAsc(INCOMING)).thenReturn(Optional.of(cr42));
        when(repo.findFirstByServiceIdentifierAndDirectionOrderByLastUpdateAsc(DPO, INCOMING)).thenReturn(Optional.of(cr42));

        Conversation c42 = Conversation.of(cr42);
        Conversation c43 = Conversation.of(cr43);
        Conversation c44 = Conversation.of(cr44);
        when(conversationService.registerStatus(eq("42"), any(MessageStatus.class))).thenReturn(Optional.of(c42));
        when(conversationService.registerStatus(eq("43"), any(MessageStatus.class))).thenReturn(Optional.of(c43));
        when(conversationService.registerStatus(eq("44"), any(MessageStatus.class))).thenReturn(Optional.of(c44));
    }

    @Test
    public void popMessageShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("bar"));
    }

    @Test
    public void readMessageShouldLock() throws Exception {
        mvc.perform(post("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("bar"));

        mvc.perform(post("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void unlockMessageShouldReturnOk() throws Exception {
        mvc.perform(post("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("bar"));

        mvc.perform(post("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("bar"));

    }

    @Test
    public void deleteMessageShouldReturnOk() throws Exception {
        mvc.perform(delete("/in/messages/pop")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    public void getMessageWithIdShouldReturnOk() throws Exception {
        mvc.perform(get("/in/messages")
                .accept(MediaType.APPLICATION_JSON)
                .param("conversationId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(10)))
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
                .andExpect(jsonPath("$.*", hasSize(10)))
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
                .andExpect(jsonPath("$.*", hasSize(10)))
                .andExpect(jsonPath("$.conversationId", is("42")))
                .andExpect(jsonPath("$.senderId", is("2")))
                .andExpect(jsonPath("$.receiverId", is("1")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
    }
}
