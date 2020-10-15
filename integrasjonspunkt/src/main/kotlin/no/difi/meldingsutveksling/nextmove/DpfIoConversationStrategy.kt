package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.api.DpfioConversationStrategy
import no.difi.meldingsutveksling.ks.fiksio.FiksIoService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class DpfIoConversationStrategyImpl(private val fiksIoService: FiksIoService) : DpfioConversationStrategy {

    override fun send(message: NextMoveOutMessage) {
        fiksIoService.sendMessage(message)
    }

}