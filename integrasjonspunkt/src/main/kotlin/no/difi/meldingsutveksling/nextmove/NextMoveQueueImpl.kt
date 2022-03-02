package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.MessagePersister
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.sbdh.SBDService
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.dpo.MessageChannelEntry
import no.difi.meldingsutveksling.dpo.MessageChannelRepository
import no.difi.meldingsutveksling.logging.Audit
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.util.logger
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream


@Component
open class NextMoveQueueImpl(private val messageRepo: NextMoveMessageInRepository,
                             private val conversationService: ConversationService,
                             private val sbdService: SBDService,
                             private val messagePersister: MessagePersister,
                             private val timeToLiveHelper: TimeToLiveHelper,
                             private val statusSender: ResponseStatusSender,
                             private val messageChannelRepository: MessageChannelRepository) : NextMoveQueue {

    val log = logger()

    override fun enqueueIncomingStatus(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier) {
        enqueueIncomingMessage(sbd, serviceIdentifier, null)
    }

    @Transactional
    override fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier, asicStream: InputStream?) {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.messageId)
        when {
            sbd.any !is BusinessMessage<*> -> throw MeldingsUtvekslingRuntimeException("SBD payload not of a known type")
            sbdService.isExpired(sbd) -> {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING)
                asicStream?.close()
                return
            }
            SBDUtil.isStatus(sbd) -> {
                log.debug("Message with id=${sbd.messageId} is a receipt")
                conversationService.registerStatus(sbd.messageId, (sbd.any as StatusMessage).status)
                return
            }
        }

        asicStream?.use { messagePersister.writeStream(sbd.messageId, NextMoveConsts.ASIC_FILE, it, -1L) }

        val message = messageRepo.findByMessageId(sbd.messageId).orElseGet {
            messageRepo.save(NextMoveInMessage.of(sbd, serviceIdentifier))
        }

        SBDUtil.getOptionalMessageChannel(sbd).ifPresent {
            messageChannelRepository.save(MessageChannelEntry(sbd.messageId, it.identifier))
        }
        conversationService.registerConversation(sbd,
                                                 serviceIdentifier,
                                                 ConversationDirection.INCOMING,
                                                 ReceiptStatus.INNKOMMENDE_MOTTATT)
        statusSender.queue(message.sbd, serviceIdentifier, ReceiptStatus.MOTTATT)

        Audit.info("Message [id=${message.messageId}, serviceIdentifier=$serviceIdentifier] put on local queue",
                   markerFrom(message)
        )
    }

}