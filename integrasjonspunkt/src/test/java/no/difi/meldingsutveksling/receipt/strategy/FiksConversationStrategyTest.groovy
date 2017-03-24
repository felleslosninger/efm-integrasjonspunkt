package no.difi.meldingsutveksling.receipt.strategy

import no.difi.meldingsutveksling.ks.SvarUtService
import no.difi.meldingsutveksling.receipt.Conversation
import no.difi.meldingsutveksling.receipt.ConversationRepository
import no.difi.meldingsutveksling.receipt.MessageReceipt
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.time.LocalDateTime

import static org.mockito.Matchers.any
import static org.mockito.Mockito.*

@RunWith(JUnit4)
class FiksConversationStrategyTest {
    private SvarUtService service

    @Before
    void setup() {
        service = mock(SvarUtService)

    }

    @Test
    void "given message receipt with status read then conversation should be set non pollable"() {
        def messageReceipt = MessageReceipt.of(ReceiptStatus.READ, LocalDateTime.now())
        Conversation conversation = mock(Conversation)
        FiksConversationStrategy strategy = new FiksConversationStrategy(service, mock(ConversationRepository))
        when(service.getMessageReceipt(any(Conversation))).thenReturn(messageReceipt)

        strategy.checkStatus(conversation)

        verify(conversation).setPollable(false)
    }
}
