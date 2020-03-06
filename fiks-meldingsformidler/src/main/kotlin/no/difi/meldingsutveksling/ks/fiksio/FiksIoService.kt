package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.pipes.Plumber
import no.difi.meldingsutveksling.pipes.PromiseMaker
import no.difi.meldingsutveksling.util.logger
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.model.KontoId
import no.ks.fiks.io.client.model.MeldingRequest
import no.ks.fiks.io.client.model.Payload
import no.ks.fiks.io.client.model.StreamPayload
import org.springframework.stereotype.Component
import java.util.*
import java.util.function.Consumer

@Component
class FiksIoService(private val props: IntegrasjonspunktProperties,
                    private val fiksIoKlient: FiksIOKlient,
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
            sendMessage(payloads = payloads)
        }
    }

    fun sendMessage(meldingType: String = "no.difi.einnsyn.innsynskrav.v1", payloads: List<Payload>) {
        val kontoId: UUID = UUID.fromString(props.fiks.io.kontoId)
        val request = MeldingRequest.builder()
                // TODO receive kontoId
                .mottakerKontoId(KontoId(kontoId))
                .meldingType(meldingType)
                .build()
        val sentMessage = fiksIoKlient.send(request, payloads)
        log.debug("FiksIO: Sent message with fiksId=${sentMessage.meldingId}")
    }

}