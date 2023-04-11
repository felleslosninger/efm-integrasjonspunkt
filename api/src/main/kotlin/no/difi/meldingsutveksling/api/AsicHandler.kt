package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.dokumentpakking.domain.Document
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.move.common.io.pipe.Reject
import org.springframework.core.io.Resource
import org.springframework.core.io.WritableResource
import java.security.cert.X509Certificate
import java.util.stream.Stream

interface AsicHandler {

    fun createCmsEncryptedAsice(msg: NextMoveMessage, writableResource: WritableResource)

    fun createCmsEncryptedAsice(msg: NextMoveMessage, reject: Reject): Resource

    fun createCmsEncryptedAsice(
        msg: NextMoveMessage,
        mainDocument: Document,
        attachments: Stream<Document>,
        certificate: X509Certificate,
        reject: Reject
    ): Resource
}