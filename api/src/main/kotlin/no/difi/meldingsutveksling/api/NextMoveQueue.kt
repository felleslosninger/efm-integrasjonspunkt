package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import org.springframework.core.io.Resource

interface NextMoveQueue {

    /**
     * Enqueue an incoming nextmove message. [asic] is closed.
     */
    fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier, asic: Resource?)

    /**
     * Enqueue an incoming message.
     */
    fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier)
}