package no.difi.meldingsutveksling.nextmove

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.difi.meldingsutveksling.DocumentType
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class ResponseStatusSender(
    val proxy: ResponseStatusSenderProxy,
    val props: IntegrasjonspunktProperties
) {
    val log = logger()

    fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        if (si in props.nextmove.statusServices) {
            GlobalScope.launch(CoroutineExceptionHandler { _, t -> log.error("Error sending status message", t) }) {
                proxy.queue(sbd, si, status)
            }
        }
    }

}

@Component
open class ResponseStatusSenderProxy(
    @Lazy val internalQueue: InternalQueue,
    private val receiptFactory: SBDReceiptFactory
) {

    @Retryable(maxAttempts = 10, backoff = Backoff(delay = 5000, multiplier = 2.0, maxDelay = 1000 * 60 * 10))
    open fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        receiptFactory.createStatusFrom(sbd, DocumentType.STATUS, status)
            ?.let { NextMoveOutMessage.of(it, si) }
            ?.let(internalQueue::enqueueNextMove)
    }
}
