package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimeToLiveHelper {

    private final ConversationService conversationService;
    private final MessageStatusFactory messageStatusFactory;

    @Transactional
    public void registerErrorStatusAndMessage(Conversation c) {
        conversationService.registerStatus(c.getMessageId(),
                messageStatusFactory.getMessageStatus(ReceiptStatus.LEVETID_UTLOPT,
                        "Levetiden for meldingen er utgått. Må sendes på nytt"));
    }

    @Transactional
    public void registerErrorStatusAndMessage(StandardBusinessDocument sbd, ServiceIdentifier si, ConversationDirection direction) {
        registerErrorStatusAndMessage(conversationService.registerConversation(sbd, si, direction));
    }
}