package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument

interface NextMoveQueue {
    fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier)
}