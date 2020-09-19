package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.DocumentType
import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.ServiceIdentifier.DPE
import no.difi.meldingsutveksling.ServiceIdentifier.DPO
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.MessagePersister
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory
import no.difi.meldingsutveksling.logging.Audit
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.status.MessageStatusFactory
import no.difi.meldingsutveksling.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream


@Component
open class NextMoveQueueImpl(private val messageRepo: NextMoveMessageInRepository,
                             private val conversationService: ConversationService,
                             private val messageStatusFactory: MessageStatusFactory,
                             @Lazy private val internalQueue: InternalQueue,
                             private val receiptFactory: SBDReceiptFactory,
                             private val sbdUtil: SBDUtil,
                             private val messagePersister: MessagePersister,
                             private val timeToLiveHelper: TimeToLiveHelper) : NextMoveQueue {

    val log = logger()

    @Transactional
    override fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier, asicStream: InputStream?) {
        if (sbd.any !is BusinessMessage<*>) {
            throw MeldingsUtvekslingRuntimeException("SBD payload not of a known type")
        }

        if (sbdUtil.isExpired(sbd)) {
            timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING)
            asicStream?.close()
            return
        }
        if (sbdUtil.isStatus(sbd)) {
            log.debug("Message with id=${sbd.documentId} is a receipt")
            conversationService.registerStatus(sbd.documentId, messageStatusFactory.getMessageStatus((sbd.any as StatusMessage).status))
            return
        }

        asicStream?.use { messagePersister.writeStream(sbd.documentId, NextMoveConsts.ASIC_FILE, it, -1L) }

        val message = messageRepo.findByMessageId(sbd.documentId).orElseGet {
            messageRepo.save(NextMoveInMessage.of(sbd, serviceIdentifier))
        }

        conversationService.registerConversation(sbd, serviceIdentifier, ConversationDirection.INCOMING, ReceiptStatus.INNKOMMENDE_MOTTATT)

        if (DPO == serviceIdentifier) {
            val statusSbd = receiptFactory.createArkivmeldingStatusFrom(message.sbd, DocumentType.STATUS, ReceiptStatus.MOTTATT)
            val msg = NextMoveOutMessage.of(statusSbd, DPO)
            internalQueue.enqueueNextMove(msg)
        }
        if (DPE == serviceIdentifier) {
            val statusSbd = receiptFactory.createEinnsynStatusFrom(message.sbd, DocumentType.STATUS, ReceiptStatus.MOTTATT)
            val msg = NextMoveOutMessage.of(statusSbd, DPE)
            internalQueue.enqueueNextMove(msg)
        }

        Audit.info("Message [id=${message.messageId}, serviceIdentifier=$serviceIdentifier] put on local queue", markerFrom(message))
    }
}