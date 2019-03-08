package no.difi.meldingsutveksling.receipt.service;

import com.querydsl.core.types.Predicate;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ConversationController.class)
@EnableSpringDataWebSupport
public class ConversationControllerTest {

    private final static LocalDateTime NOW = LocalDateTime.now();
    private final static LocalDateTime NOW_MINUS_5_MIN = LocalDateTime.now().minusMinutes(5);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ConversationRepository convoRepo;

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

        Conversation c1 = Conversation.of(cId1, "foo", "24", "42", OUTGOING, "foo", ServiceIdentifier.DPO, cId1ms1, cId1ms2);
        c1.setConvId(1);
        c1.setPollable(true);
        c1.setLastUpdate(NOW_MINUS_5_MIN);
        Conversation c2 = Conversation.of(cId2, "foo", "24", "43", OUTGOING, "foo", ServiceIdentifier.DPO, cId2ms1, cId2ms2, cId2ms3);
        c2.setConvId(2);
        c2.setPollable(false);
        c2.setLastUpdate(NOW);

        when(convoRepo.findAll(org.mockito.Matchers.any(Predicate.class), org.mockito.Matchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(asList(c1, c2)));
        when(convoRepo.findByReceiverIdentifierAndDirection("43", OUTGOING)).thenReturn(singletonList(c2));
        when(convoRepo.findByDirection(OUTGOING)).thenReturn(asList(c1, c2));
        when(convoRepo.findByConvIdAndDirection(1, OUTGOING)).thenReturn(Optional.of(c1));
        when(convoRepo.findByConvIdAndDirection(2, OUTGOING)).thenReturn(Optional.of(c2));
        when(convoRepo.findByConversationIdAndDirection("123", OUTGOING)).thenReturn(singletonList(c1));
        when(convoRepo.findByPollable(org.mockito.Matchers.eq(true), org.mockito.Matchers.any(Pageable.class))).thenReturn(new PageImpl<>(singletonList(c1)));
        when(convoRepo.findByPollable(org.mockito.Matchers.eq(false), org.mockito.Matchers.any(Pageable.class))).thenReturn(new PageImpl<>(singletonList(c2)));
    }

    @Test
    public void conversationsTest() throws Exception {
        mvc.perform(get("/conversations")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].convId", containsInAnyOrder(1, 2)));
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
    public void conversationsWithConversationIdParamTest() throws Exception {
        mvc.perform(get("/conversations/conversationId/123")
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
    public void conversationsWithNonNumericIdTest() throws Exception {
        mvc.perform(get("/conversations/asd123")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void conversationsQueueTest() throws Exception {
        mvc.perform(get("/conversations/queue")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[*].convId", Matchers.contains(1)));
    }
}
