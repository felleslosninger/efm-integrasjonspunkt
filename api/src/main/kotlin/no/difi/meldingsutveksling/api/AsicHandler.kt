package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.dokumentpakking.domain.Document
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.pipes.Reject
import org.springframework.core.io.InputStreamResource
import java.security.cert.X509Certificate
import java.util.stream.Stream

interface AsicHandler {

    fun createEncryptedAsic(msg: NextMoveMessage, reject: Reject): InputStreamResource

    fun createEncryptedAsic(
        msg: NextMoveMessage,
        mainDocument: Document,
        attachments: Stream<Document>,
        certificate: X509Certificate,
        reject: Reject
    ): InputStreamResource
}