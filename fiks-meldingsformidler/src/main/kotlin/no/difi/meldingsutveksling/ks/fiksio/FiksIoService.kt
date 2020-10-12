package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.pipes.Plumber
import no.difi.meldingsutveksling.pipes.PromiseMaker
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
import java.util.function.Consumer

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class FiksIoService(private val fiksIoKlient: FiksIOKlient,
                    private val serviceRegistryLookup: ServiceRegistryLookup,
                    private val persister: OptionalCryptoMessagePersister,
                    private val objectMapper: ObjectMapper,
                    private val plumber: Plumber,
                    private val promise: PromiseMaker) {
    val log = logger()


    fun sendMessage(msg: NextMoveMessage) {
        promise.promise { reject ->
            val payloads = msg.files.map {
                StreamPayload(persister.readStream(msg.messageId, it.identifier, reject).inputStream, it.filename)
            }.toCollection(arrayListOf())
            objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
            val outlet = plumber.pipe("Write SBD to FiksIO StreamPayload", Consumer {
                objectMapper.writeValue(it, msg.sbd)
            }, reject).outlet()
            payloads.add(StreamPayload(outlet, NextMoveConsts.SBD_FILE))
            createRequest(payloads = payloads, msg = msg)
        }
    }

    private fun createRequest(msg: NextMoveMessage, payloads: List<Payload>) {
        val serviceRecord: ServiceRecord = serviceRegistryLookup.getServiceRecord(
                SRParameter.builder(msg.receiverIdentifier)
                        .process(msg.sbd.process)
                        .conversationId(msg.conversationId).build(),
                msg.sbd.standard)
        val request = MeldingRequest.builder()
                .mottakerKontoId(KontoId(UUID.fromString(serviceRecord.service.endpointUrl)))
                .meldingType(serviceRecord.service.serviceCode)
                .build()
        val sentMessage = fiksIoKlient.send(request, payloads)
        log.debug("FiksIO: Sent message with fiksId=${sentMessage.meldingId}")
    }

}