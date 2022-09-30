package no.difi.meldingsutveksling.dpi.json;

import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MessageStatusRawReceiptXmlDecorator.class)
public class MessageStatusRawReceiptXmlDecoratorTest {

    @Autowired
    private MessageStatusRawReceiptXmlDecorator messageStatusRawReceiptXmlDecorator;

    @Test
    public void test() {
        Conversation conversation = new Conversation();
        conversation.setMessageId(UUID.randomUUID().toString());
        conversation.setConversationId(UUID.randomUUID().toString());
        MessageStatus messageStatus = MessageStatus.of(ReceiptStatus.FEIL, OffsetDateTime.now(), "Oh no");
        MessageStatus messageStatus2 = messageStatusRawReceiptXmlDecorator.apply(conversation, messageStatus);
        System.out.println(messageStatus2.getRawReceipt());
    }
}
