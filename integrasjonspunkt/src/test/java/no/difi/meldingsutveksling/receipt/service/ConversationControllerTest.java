package no.difi.meldingsutveksling.receipt.service;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.*;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ConversationController.class)
public class ConversationControllerTest {

    final static LocalDateTime NOW = LocalDateTime.now();
    final static LocalDateTime NOW_MINUS_5_MIN = LocalDateTime.now().minusMinutes(5);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConversationRepository convoRepo;

    @MockBean
    private MessageStatusRepository statRepo;


    @Before
    public void setup() {

        final String cId1 = "123";
        final String cId2 = "321";

        MessageStatus cId1ms1 = MessageStatus.of(GenericReceiptStatus.SENDT, NOW_MINUS_5_MIN);
        cId1ms1.setStatId(1);
        cId1ms1.setConvId(1);
        MessageStatus cId1ms2 = MessageStatus.of(GenericReceiptStatus.LEVERT, NOW_MINUS_5_MIN);
        cId1ms2.setStatId(2);
        cId1ms2.setConvId(1);

        MessageStatus cId2ms1 = MessageStatus.of(GenericReceiptStatus.SENDT, NOW);
        cId2ms1.setStatId(3);
        cId2ms1.setConvId(2);
        MessageStatus cId2ms2 = MessageStatus.of(GenericReceiptStatus.LEVERT, NOW);
        cId2ms2.setStatId(4);
        cId2ms2.setConvId(2);
        MessageStatus cId2ms3 = MessageStatus.of(GenericReceiptStatus.LEST, NOW);
        cId2ms3.setStatId(5);
        cId2ms3.setConvId(2);

        Conversation c1 = Conversation.of(cId1, "foo", "42", "foo", ServiceIdentifier.DPO, cId1ms1, cId1ms2);
        c1.setConvId(1);
        c1.setPollable(true);
        c1.setLastUpdate(NOW_MINUS_5_MIN);
        Conversation c2 = Conversation.of(cId2, "foo", "42", "foo", ServiceIdentifier.DPO, cId2ms1, cId2ms2, cId2ms3);
        c2.setConvId(2);
        c2.setPollable(false);
        c2.setLastUpdate(NOW);

        when(convoRepo.findAll()).thenReturn(asList(c1, c2));
        when(convoRepo.findByConvId(1)).thenReturn(Optional.of(c1));
        when(convoRepo.findByConvId(2)).thenReturn(Optional.of(c2));
        when(convoRepo.findByPollable(true)).thenReturn(asList(c1));
        when(convoRepo.findByPollable(false)).thenReturn(asList(c2));

        when(statRepo.findAll()).thenReturn(asList(cId1ms1, cId1ms2, cId2ms1, cId2ms2, cId2ms3));
        when(statRepo.findAllByConvId(1)).thenReturn(asList(cId1ms1, cId1ms2));
        when(statRepo.findByStatIdGreaterThanEqual(3)).thenReturn(asList(cId2ms1, cId2ms2, cId2ms3));
        when(statRepo.findAllByConvIdAndStatIdGreaterThanEqual(2, 4)).thenReturn(asList(cId2ms2, cId2ms3));
        when(statRepo.findFirstByOrderByLastUpdateAsc()).thenReturn(Optional.of(cId2ms3));
    }

    @Test
    public void conversationsTest() throws Exception {
        mvc.perform(get("/conversations")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].convId", containsInAnyOrder(1, 2)));
    }

    @Test
    public void conversationsWithIdParamTest() throws Exception {
        mvc.perform(get("/conversations/1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convId", is(1)))
                .andExpect(jsonPath("$.conversationId", is("123")))
                .andExpect(jsonPath("$.receiverIdentifier", is("42")))
                .andExpect(jsonPath("$.messageReference", is("foo")))
                .andExpect(jsonPath("$.messageTitle", is("foo")))
                .andExpect(jsonPath("$.serviceIdentifier", is("DPO")));
    }

    @Test
    public void conversationsQueueTest() throws Exception {
        mvc.perform(get("/conversations/queue")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].convId", Matchers.contains(1)));
    }

    @Test
    public void statusesTest() throws Exception {
        mvc.perform(get("/statuses")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].convId", contains(1, 1, 2, 2, 2)));
    }

    @Test
    public void statusesWithConvIdParamTest() throws Exception {
        mvc.perform(get("/statuses")
                .param("convId", "1")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].convId", contains(1, 1)))
                .andExpect(jsonPath("$[*].statId", containsInAnyOrder(1, 2)));
    }

    @Test
    public void statusesWithFromIdParamTest() throws Exception {
        mvc.perform(get("/statuses")
                .param("fromId", "3")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].convId", contains(2, 2, 2)))
                .andExpect(jsonPath("$[*].statId", containsInAnyOrder(3, 4, 5)));
    }

    @Test
    public void statusesWithConvIdAndFromIdParamTest() throws Exception {
        mvc.perform(get("/statuses")
                .param("convId", "2")
                .param("fromId", "4")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].convId", contains(2, 2)))
                .andExpect(jsonPath("$[*].statId", containsInAnyOrder(4, 5)));
    }

    @Test
    public void statusesPeekTest() throws Exception {
        mvc.perform(get("/statuses/peek")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convId", is(2)))
                .andExpect(jsonPath("$.conversationId", is("321")));
    }
}
