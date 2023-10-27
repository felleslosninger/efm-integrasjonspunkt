package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;

import java.time.OffsetDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

@UtilityClass
class MessageStatusTestData {

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

    static MessageStatus messageStatus1() {
        return getMessageStatus(new MessageStatusDTO()
                .setId(1L)
                .setConvId(2L)
                .setMessageId("1cc3fb67-b776-4730-b017-1028b86a8b8b")
                .setConversationId("cc3740ec-c6c1-474f-a93d-7e73816ca34b")
                .setStatus(ReceiptStatus.MOTTATT.toString())
                .setLastUpdate(OffsetDateTime.parse("2019-11-05T12:04:34+02:00"))
                .setDescription("Mottatt")
                .setRawReceipt("The raw receipt"));
    }

    static MessageStatus messageStatus2() {
        return getMessageStatus(new MessageStatusDTO()
                .setId(7L)
                .setConvId(4L)
                .setMessageId("e424303b-9d8d-4392-b02e-14da4d3dad36")
                .setConversationId("4364a1f2-be6a-46f2-832d-c11d9b52abad")
                .setStatus(ReceiptStatus.LEVERT.toString())
                .setLastUpdate(OffsetDateTime.parse("2019-10-23T15:43:12+02:00"))
                .setDescription("Levert")
                .setRawReceipt("The raw receipt 2"));
    }

    static MessageStatus messageStatus3() {
        return getMessageStatus(new MessageStatusDTO()
                .setId(12L)
                .setConvId(6L)
                .setMessageId("49c5d26b-6fef-4259-991e-84497288673e")
                .setConversationId("22ea640b-96ab-4c1b-98a8-950ab254a501")
                .setStatus(ReceiptStatus.LEVERT.toString())
                .setLastUpdate(OffsetDateTime.parse("2023-09-17T15:43:12+02:00"))
                .setDescription("Levert")
                .setRawReceipt("The raw receipt 3"));
    }

    static MessageStatus messageStatus(Long id, ReceiptStatus status, Conversation conversation) {
        return getMessageStatus(new MessageStatusDTO()
                .setId(id)
                .setConvId(conversation.getId())
                .setMessageId(conversation.getMessageId())
                .setConversationId(conversation.getConversationId())
                .setStatus(status.toString())
                .setLastUpdate(OffsetDateTime.parse("2019-11-05T12:04:34+02:00"))
                .setDescription(status.toString())
                .setRawReceipt("The raw receipt"));
    }

    private static MessageStatus getMessageStatus(MessageStatusDTO dto) {
        MessageStatus messageStatus = spy(MessageStatus.of(ReceiptStatus.valueOf(dto.getStatus()), dto.getLastUpdate(), dto.getDescription())
                .setConversation(new Conversation()
                        .setMessageId(dto.getMessageId())
                        .setConversationId(dto.getConversationId())
                ));

        given(messageStatus.getId()).willReturn(dto.getId());
        given(messageStatus.getConvId()).willReturn(dto.getConvId());
        return messageStatus;
    }


}
