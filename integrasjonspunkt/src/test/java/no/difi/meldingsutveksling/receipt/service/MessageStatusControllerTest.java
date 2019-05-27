package no.difi.meldingsutveksling.receipt.service;

import com.querydsl.core.types.Predicate;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusQueryInput;
import no.difi.meldingsutveksling.receipt.MessageStatusRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
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
@WebMvcTest(MessageStatusController.class)
@EnableSpringDataWebSupport
@Import(SystemClockConfig.class)
public class MessageStatusControllerTest {

    private final static LocalDateTime NOW = LocalDateTime.now();
    private final static LocalDateTime NOW_MINUS_5_MIN = LocalDateTime.now().minusMinutes(5);

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageStatusRepository statRepo;

    @Before
    public void setup() {
        MessageStatus cId1ms1 = MessageStatus.of(ReceiptStatus.SENDT, NOW_MINUS_5_MIN);
        cId1ms1.setStatId(1);
        cId1ms1.setConvId(1);
        MessageStatus cId1ms2 = MessageStatus.of(ReceiptStatus.LEVERT, NOW_MINUS_5_MIN);
        cId1ms2.setStatId(2);
        cId1ms2.setConvId(1);

        MessageStatus cId2ms1 = MessageStatus.of(ReceiptStatus.SENDT, NOW);
        cId2ms1.setStatId(3);
        cId2ms1.setConvId(2);
        MessageStatus cId2ms2 = MessageStatus.of(ReceiptStatus.LEVERT, NOW);
        cId2ms2.setStatId(4);
        cId2ms2.setConvId(2);
        MessageStatus cId2ms3 = MessageStatus.of(ReceiptStatus.LEST, NOW);
        cId2ms3.setStatId(5);
        cId2ms3.setConvId(2);
        cId2ms3.setConversationId("321");


        when(statRepo.find(ArgumentMatchers.any(MessageStatusQueryInput.class), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(asList(cId1ms1, cId1ms2, cId2ms1, cId2ms2, cId2ms3)));
        when(statRepo.findAll(ArgumentMatchers.any(Predicate.class), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(asList(cId1ms1, cId1ms2, cId2ms1, cId2ms2, cId2ms3)));
        when(statRepo.findAll(ArgumentMatchers.isNull(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(asList(cId1ms1, cId1ms2, cId2ms1, cId2ms2, cId2ms3)));
        when(statRepo.findAllByConvId(1)).thenReturn(asList(cId1ms1, cId1ms2));
        when(statRepo.findByStatIdGreaterThanEqual(3)).thenReturn(asList(cId2ms1, cId2ms2, cId2ms3));
        when(statRepo.findAllByConvIdAndStatIdGreaterThanEqual(2, 4)).thenReturn(asList(cId2ms2, cId2ms3));
        when(statRepo.findFirstByOrderByLastUpdateAsc()).thenReturn(Optional.of(cId2ms3));
    }

    @Test
    public void statusesTest() throws Exception {
        mvc.perform(get("/api/statuses")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[*].convId", contains(1, 1, 2, 2, 2)));
    }


    @Test
    public void statusesPeekTest() throws Exception {
        mvc.perform(get("/api/statuses/peek")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convId", is(2)))
                .andExpect(jsonPath("$.conversationId", is("321")));
    }
}
