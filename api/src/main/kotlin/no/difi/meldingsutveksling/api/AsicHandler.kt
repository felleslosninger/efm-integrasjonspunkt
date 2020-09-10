package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.domain.StreamedFile
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.pipes.Reject
import java.io.InputStream
import java.security.cert.X509Certificate
import java.util.stream.Stream

interface AsicHandler {
    fun createEncryptedAsic(msg: NextMoveMessage, reject: Reject): InputStream
    fun archiveAndEncryptAttachments(mainAttachment: StreamedFile, attachments: Stream<out StreamedFile>, msg: NextMoveMessage, cert: X509Certificate, reject: Reject): InputStream
}