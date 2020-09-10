package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import java.io.InputStream

interface NextMoveQueue {
    fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier, asicStream: InputStream?)
}