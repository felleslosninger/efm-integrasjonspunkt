package no.difi.meldingsutveksling.nextmove

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.sbd.SBDFactory
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class ResponseStatusSender(
    val proxy: ResponseStatusSenderProxy,
    val props: IntegrasjonspunktProperties
) {
    val log = LoggerFactory.getLogger(ResponseStatusSender::class.java)

    fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        if (si in props.nextmove.statusServices) {
            CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, t -> log.error("Error sending status message", t) }) {
                proxy.queue(sbd, si, status)
            }
        }
    }

}

@Component
open class ResponseStatusSenderProxy(
    @Lazy val internalQueue: InternalQueue,
    private val sbdFactory: SBDFactory
) {

    @Retryable(maxAttempts = 10, backoff = Backoff(delay = 5000, multiplier = 2.0, maxDelay = 1000 * 60 * 10L))
    open fun queue(sbd: StandardBusinessDocument, si: ServiceIdentifier, status: ReceiptStatus) {
        sbdFactory.createStatusFrom(sbd, status)
            ?.let { NextMoveOutMessage.of(it, si) }
            ?.let(internalQueue::enqueueNextMove)
    }
}
