package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.ks.fiksio.FiksIoService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class DpfIoConversationStrategy(private val fiksIoService: FiksIoService) : ConversationStrategy {

    override fun send(message: NextMoveOutMessage) {
        fiksIoService.sendMessage(message)
    }

}