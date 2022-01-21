package no.difi.meldingsutveksling.nextmove

import io.micrometer.core.annotation.Timed
import no.difi.meldingsutveksling.api.DpfioConversationStrategy
import no.difi.meldingsutveksling.ks.fiksio.FiksIoService
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers
import no.difi.meldingsutveksling.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
open class DpfIoConversationStrategyImpl(
    private val fiksIoService: FiksIoService,
    private val nextMoveMessageMarkers: NextMoveMessageMarkers
) : DpfioConversationStrategy {

    val log = logger()

    @Timed
    override fun send(message: NextMoveOutMessage) {
        fiksIoService.sendMessage(message)
        log.info("Message [id=${message.messageId}, serviceIdentifier=${message.serviceIdentifier}] sent to FIKS IO",
            nextMoveMessageMarkers.markerFrom(message))
    }

}