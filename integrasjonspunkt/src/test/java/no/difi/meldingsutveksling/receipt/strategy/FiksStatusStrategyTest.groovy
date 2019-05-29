package no.difi.meldingsutveksling.receipt.strategy

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.ks.svarut.SvarUtService
import no.difi.meldingsutveksling.nextmove.ConversationDirection
import no.difi.meldingsutveksling.receipt.Conversation
import no.difi.meldingsutveksling.receipt.ConversationService
import no.difi.meldingsutveksling.receipt.MessageStatus
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import spock.lang.Specification
import spock.lang.Unroll

import java.time.ZonedDateTime

class FiksStatusStrategyTest extends Specification {

    @Unroll
    "given message receipt with status #status, conversation should be set non pollable"() {
        given:
        SvarUtService svarUtService = Mock(SvarUtService)
        ConversationService conversationService = Mock(ConversationService)
        Conversation conversation = Mock(Conversation)

        when:
        def messageReceipt = MessageStatus.of(status, ZonedDateTime.now())
        FiksStatusStrategy strategy = new FiksStatusStrategy(svarUtService, conversationService)
        svarUtService.getMessageReceipt(_) >> messageReceipt
        conversationService.registerStatus(_ as Conversation, _ as MessageStatus) >> conversation
        conversation.getReceiverIdentifier() >> "123456785"
        conversation.getServiceIdentifier() >> ServiceIdentifier.DPF
        conversation.getDirection() >> ConversationDirection.OUTGOING
        conversation.getConversationId() >> UUID.randomUUID().toString()

        strategy.checkStatus(conversation)

        then:
        1 * conversationService.markFinished(_)

        where:
        status << [ReceiptStatus.LEST, ReceiptStatus.FEIL]
    }


}
