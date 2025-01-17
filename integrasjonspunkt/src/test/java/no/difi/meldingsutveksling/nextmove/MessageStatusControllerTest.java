package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusQueryInput;
import no.difi.meldingsutveksling.status.MessageStatusRepository;
import no.difi.meldingsutveksling.status.service.MessageStatusController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.MessageStatusTestData.*;
import static no.difi.meldingsutveksling.nextmove.RestDocumentationCommon.*;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(MessageStatusController.class)
@AutoConfigureMoveRestDocs
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
public class MessageStatusControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageStatusRepository statusRepo;

    @MockBean
    private StatusQueue statusQueue;

    @Test
    public void find() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Arrays.asList(messageStatus1(), messageStatus2());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].convId", is(2)))
                .andExpect(jsonPath("$.content[0].lastUpdate", is("2019-11-05T12:04:34+02:00")))
                .andExpect(jsonPath("$.content[0].status", is("MOTTATT")))
                .andExpect(jsonPath("$.content[0].description", is("Mottatt")))
                .andExpect(jsonPath("$.content[0].messageId", is("1cc3fb67-b776-4730-b017-1028b86a8b8b")))
                .andExpect(jsonPath("$.content[0].conversationId", is("cc3740ec-c6c1-474f-a93d-7e73816ca34b")))
                .andDo(document("statuses/find",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("id").optional().description("Filter on the numeric message status ID"),
                                parameterWithName("messageId").optional().description("Filter on messageId"),
                                parameterWithName("conversationId").optional().description("Filter on conversationId"),
                                parameterWithName("status").optional().description("Filter on status. Can be one of: %s".formatted(Arrays.stream(ReceiptStatus.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .and(messageStatusDescriptors("content[]."))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSearch() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Collections.singletonList(messageStatus1());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .param("messageId", "1cc3fb67-b776-4730-b017-1028b86a8b8b")
                        .param("status", "MOTTATT")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("statuses/find/search"));

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSorting() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Arrays.asList(messageStatus2(), messageStatus1());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .param("sort", "lastUpdated,asc")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("statuses/find/sorting"));

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findPaging() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Collections.singletonList(messageStatus2());
                    return new PageImpl<>(content, invocation.getArgument(1), 31L);
                });

        mvc.perform(
                get("/api/statuses")
                        .param("page", "3")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("statuses/find/paging"));

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    void testFindMessageStatusInDateRange() throws Exception {
        OffsetDateTime fromDateTime = OffsetDateTime.parse("2021-06-09T10:18:40.868+02:00");
        OffsetDateTime toDateTime = OffsetDateTime.parse("2023-06-10T10:18:41.121+02:00");

        List<MessageStatus> mockData = Arrays.asList(
                messageStatus1(),
                messageStatus2(),
                messageStatus3()
        );

        Pageable pageable = Pageable.unpaged();
        Page<MessageStatus> page = new PageImpl<>(mockData, pageable, mockData.size());

        when(statusRepo.find(
                any(MessageStatusQueryInput.class),
                any(Pageable.class)
        )).thenReturn(page);

        MvcResult mvcResult = mvc.perform(get("/api/statuses")
                        .param("fromDateTime", fromDateTime.toString())
                        .param("toDateTime", toDateTime.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String jsonResponse = mvcResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);

        assertEquals(1, jsonNode.get("totalPages").asInt());
        assertEquals(3, jsonNode.get("totalElements").asInt());
    }

    @Test
    public void findByMessageId() throws Exception {
        given(statusRepo.findByConversationMessageId(any(String.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Collections.singletonList(messageStatus1());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses/{messageId}", "1cc3fb67-b776-4730-b017-1028b86a8b8b")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].convId", is(2)))
                .andExpect(jsonPath("$.content[0].lastUpdate", is("2019-11-05T12:04:34+02:00")))
                .andExpect(jsonPath("$.content[0].status", is("MOTTATT")))
                .andExpect(jsonPath("$.content[0].description", is("Mottatt")))
                .andExpect(jsonPath("$.content[0].messageId", is("1cc3fb67-b776-4730-b017-1028b86a8b8b")))
                .andExpect(jsonPath("$.content[0].conversationId", is("cc3740ec-c6c1-474f-a93d-7e73816ca34b")))
                .andDo(document("statuses/find-by-message-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("messageId").description("The messageId")
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .and(messageStatusDescriptors("content[]."))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(statusRepo).findByConversationMessageId(eq("1cc3fb67-b776-4730-b017-1028b86a8b8b"), any(Pageable.class));
    }

    @Test
    public void peekLatest() throws Exception {
        MessageStatus messageStatus = messageStatus1();
        given(statusQueue.receiveStatus()).willReturn(Optional.of(1L));
        given(statusRepo.findById(1L)).willReturn(Optional.of(messageStatus));

        mvc.perform(
                get("/api/statuses/peek")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.convId", is(2)))
                .andExpect(jsonPath("$.lastUpdate", is("2019-11-05T12:04:34+02:00")))
                .andExpect(jsonPath("$.status", is("MOTTATT")))
                .andExpect(jsonPath("$.description", is("Mottatt")))
                .andExpect(jsonPath("$.messageId", is("1cc3fb67-b776-4730-b017-1028b86a8b8b")))
                .andExpect(jsonPath("$.conversationId", is("cc3740ec-c6c1-474f-a93d-7e73816ca34b")))
                .andDo(document("statuses/peek-latest",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        responseFields(messageStatusDescriptors(""))
                        )
                );
    }
}