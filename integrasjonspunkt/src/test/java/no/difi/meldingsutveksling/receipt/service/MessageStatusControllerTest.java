package no.difi.meldingsutveksling.receipt.service;

import java.util.Optional;

import lombok.Data;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.receipt.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.*;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.getMessageStatusFieldDescriptors;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(MessageStatusController.class)
@AutoConfigureMoveRestDocs
@ActiveProfiles("test")
public class MessageStatusControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageStatusRepository statusRepo;

    private final MessageStatus messageStatus1 = getMessageStatus(new MessageStatusDTO()
            .setId(1L)
            .setConvId(2L)
            .setMessageId("1cc3fb67-b776-4730-b017-1028b86a8b8b")
            .setConversationId("cc3740ec-c6c1-474f-a93d-7e73816ca34b")
            .setStatus(ReceiptStatus.MOTTATT.toString())
            .setLastUpdate(OffsetDateTime.parse("2019-11-05T12:04:34+02:00"))
            .setDescription("Mottatt")
            .setRawReceipt("Th raw receipt"));

    private final MessageStatus messageStatus2 = getMessageStatus(new MessageStatusDTO()
            .setId(7L)
            .setConvId(4L)
            .setMessageId("e424303b-9d8d-4392-b02e-14da4d3dad36")
            .setConversationId("4364a1f2-be6a-46f2-832d-c11d9b52abad")
            .setStatus(ReceiptStatus.LEVERT.toString())
            .setLastUpdate(OffsetDateTime.parse("2019-10-23T15:43:12+02:00"))
            .setDescription("Levert")
            .setRawReceipt("Th raw receipt 2"));

    @Test
    public void find() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Arrays.asList(messageStatus1, messageStatus2);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                                parameterWithName("status").optional().description(String.format("Filter on status. Can be one of: %s", Arrays.stream(ReceiptStatus.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .andWithPrefix("content[].", getMessageStatusFieldDescriptors())
                                .and(getPageFieldDescriptors())
                                .andWithPrefix("pageable.", getPageableFieldDescriptors())
                        )
                );

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSearch() throws Exception {
        given(statusRepo.find(any(MessageStatusQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Collections.singletonList(messageStatus1);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .param("messageId", "1cc3fb67-b776-4730-b017-1028b86a8b8b")
                        .param("status", "MOTTATT")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                    List<MessageStatus> content = Arrays.asList(messageStatus2, messageStatus1);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses")
                        .param("sort", "lastUpdated,asc")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                    List<MessageStatus> content = Collections.singletonList(messageStatus2);
                    return new PageImpl<>(content, invocation.getArgument(1), 2L);
                });

        mvc.perform(
                get("/api/statuses")
                        .param("page", "1")
                        .param("size", "1")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("statuses/find/paging"));

        verify(statusRepo).find(any(MessageStatusQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findByMessageId() throws Exception {
        given(statusRepo.findByConversationMessageId(any(String.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<MessageStatus> content = Collections.singletonList(messageStatus1);
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/statuses/{messageId}", "1cc3fb67-b776-4730-b017-1028b86a8b8b")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                                .andWithPrefix("content[].", getMessageStatusFieldDescriptors())
                                .and(getPageFieldDescriptors())
                                .andWithPrefix("pageable.", getPageableFieldDescriptors())
                        )
                );

        verify(statusRepo).findByConversationMessageId(eq("1cc3fb67-b776-4730-b017-1028b86a8b8b"), any(Pageable.class));
    }

    @Test
    public void peekLatest() throws Exception {
        given(statusRepo.findFirstByOrderByLastUpdateAsc()).willReturn(Optional.of(messageStatus1));

        mvc.perform(
                get("/api/statuses/peek")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
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
                        responseFields(getMessageStatusFieldDescriptors())
                        )
                );

        verify(statusRepo).findFirstByOrderByLastUpdateAsc();
    }

    private MessageStatus getMessageStatus(MessageStatusDTO dto) {
        MessageStatus messageStatus = spy(MessageStatus.of(ReceiptStatus.valueOf(dto.getStatus()), dto.getLastUpdate(), dto.getDescription())
                .setConversation(new Conversation()
                        .setMessageId(dto.getMessageId())
                        .setConversationId(dto.getConversationId())
                ));

        given(messageStatus.getId()).willReturn(dto.getId());
        given(messageStatus.getConvId()).willReturn(dto.getConvId());
        return messageStatus;
    }

    @Data
    private static class MessageStatusDTO {

        private Long id;
        private Long convId;
        private String messageId;
        private String conversationId;
        private String status;
        private String description;
        private String rawReceipt;
        private OffsetDateTime lastUpdate;
    }
}