package no.difi.meldingsutveksling.ks.fiksio

import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.receipt.ReceiptStatus.*
import no.difi.meldingsutveksling.serviceregistry.SRParameter
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.meldingsutveksling.util.logger
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.model.KontoId
import no.ks.fiks.io.client.model.MeldingRequest
import no.ks.fiks.io.client.model.Payload
import no.ks.fiks.io.client.model.StreamPayload
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class FiksIoService(
    private val fiksIoKlient: FiksIOKlient,
    private val serviceRegistryLookup: ServiceRegistryLookup,
    private val persister: OptionalCryptoMessagePersister,
    private val conversationService: ConversationService,
) {
    val log = logger()

    fun sendMessage(msg: NextMoveMessage) {
        val payloads = msg.files.map {
            StreamPayload(persister.read(msg.messageId, it.identifier).inputStream, it.filename)
        }.toCollection(arrayListOf())
        createRequest(payloads = payloads, msg = msg)
    }

    fun createRequest(msg: NextMoveMessage, payloads: List<Payload>) {
        val params = SRParameter.builder(msg.receiver)
            .process(msg.sbd.process)
            .conversationId(msg.conversationId)
        if (msg.businessMessage.sikkerhetsnivaa != null) {
            params.securityLevel(msg.businessMessage.sikkerhetsnivaa)
        }
        val serviceRecord: ServiceRecord =
            serviceRegistryLookup.getServiceRecord(params.build(), msg.sbd.documentType)

        val request = MeldingRequest.builder()
            .mottakerKontoId(KontoId(UUID.fromString(serviceRecord.service.endpointUrl)))
            .meldingType(serviceRecord.process)
            .build()
        val sentMessage = fiksIoKlient.send(request, payloads)
        // No status API, so need to register all statuses at once
        conversationService.registerStatus(msg.messageId, SENDT, MOTTATT, LEVERT)
        log.debug("FiksIO: Sent message with fiksId=${sentMessage.meldingId}")
    }

}