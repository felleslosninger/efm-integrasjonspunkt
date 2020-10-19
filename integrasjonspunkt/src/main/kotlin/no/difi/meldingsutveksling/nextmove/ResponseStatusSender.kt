package no.difi.meldingsutveksling.nextmove

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.difi.meldingsutveksling.DocumentType
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class ResponseStatusSender(val proxy: ResponseStatusSenderProxy) {

    val log = logger()

    fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        GlobalScope.launch( CoroutineExceptionHandler { _, t -> log.error("Error sending status message", t) }) {
            proxy.queue(sbd, si, status)
        }
    }

}

@Component
open class ResponseStatusSenderProxy(@Lazy val internalQueue: InternalQueue,
                                     val receiptFactory: SBDReceiptFactory) {

    @Retryable(maxAttempts = 10, backoff = Backoff(delay = 5000, multiplier = 2.0, maxDelay = 1000*60*10))
    @Throws(MeldingsUtvekslingRuntimeException::class)
    open fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        when (si) {
            ServiceIdentifier.DPO -> {
                receiptFactory.createArkivmeldingStatusFrom(sbd, DocumentType.STATUS, status)
                        .let { NextMoveOutMessage.of(it, ServiceIdentifier.DPO) }
            }
            ServiceIdentifier.DPE -> {
                receiptFactory.createEinnsynStatusFrom(sbd, DocumentType.STATUS, status)
                        .let { NextMoveOutMessage.of(it, ServiceIdentifier.DPE) }
            }
            else -> null
        }?.let(internalQueue::enqueueNextMove)
    }
}
