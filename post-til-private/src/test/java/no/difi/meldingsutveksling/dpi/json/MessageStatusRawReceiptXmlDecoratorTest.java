package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = MessageStatusRawReceiptXmlDecorator.class)
public class MessageStatusRawReceiptXmlDecoratorTest {

    @Autowired
    private MessageStatusRawReceiptXmlDecorator messageStatusRawReceiptXmlDecorator;

    @Test
    public void message_status_feil_without_rawreceipt_should_be_decorated() {
        Conversation conversation = new Conversation();
        conversation.setMessageId(UUID.randomUUID().toString());
        conversation.setConversationId(UUID.randomUUID().toString());
        String errorDescription = "Oh no oh no";
        MessageStatus messageStatus = MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), errorDescription);
        MessageStatus messageStatus2 = messageStatusRawReceiptXmlDecorator.apply(conversation, messageStatus);
        assertNotNull(messageStatus2.getRawReceipt());
        assertTrue(messageStatus2.getRawReceipt().contains(errorDescription));
    }

    @Test
    public void message_status_sendt_without_rawreceipt_should_not_be_decorated() {
        Conversation conversation = new Conversation();
        conversation.setMessageId(UUID.randomUUID().toString());
        conversation.setConversationId(UUID.randomUUID().toString());
        String errorDescription = "Oh no oh no";
        MessageStatus messageStatus = MessageStatus.of(ReceiptStatus.SENDT, OffsetDateTime.now(), errorDescription);
        MessageStatus messageStatus2 = messageStatusRawReceiptXmlDecorator.apply(conversation, messageStatus);
        assertNull(messageStatus2.getRawReceipt());
    }

    @Test
    public void message_status_with_rawreceipt_should_not_be_decorated() {
        Conversation conversation = new Conversation();
        conversation.setMessageId(UUID.randomUUID().toString());
        conversation.setConversationId(UUID.randomUUID().toString());
        String errorDescription = "Oh no oh no";
        MessageStatus messageStatus = MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), errorDescription);
        String rawReceipt = "Raw receipt";
        messageStatus.setRawReceipt(rawReceipt);
        MessageStatus messageStatus2 = messageStatusRawReceiptXmlDecorator.apply(conversation, messageStatus);
        assertEquals(rawReceipt, messageStatus2.getRawReceipt());
    }

}
