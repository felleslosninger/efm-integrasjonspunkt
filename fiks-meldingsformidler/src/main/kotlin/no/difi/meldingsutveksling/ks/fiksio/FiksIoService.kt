package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.pipes.Plumber
import no.difi.meldingsutveksling.pipes.PromiseMaker
import no.difi.meldingsutveksling.util.logger
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.FiksIOKlientFactory
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.konfigurasjon.*
import no.ks.fiks.io.client.model.*
import org.springframework.stereotype.Component
import java.security.KeyStore
import java.util.*
import java.util.function.Consumer

@Component
class FiksIoService(props: IntegrasjonspunktProperties,
                    ipNokkel: IntegrasjonspunktNokkel,
                    private val persister: OptionalCryptoMessagePersister,
                    private val objectMapper: ObjectMapper,
                    private val plumber: Plumber,
                    private val promise: PromiseMaker) {
    val log = logger()

    var fiksIoKlient: FiksIOKlient
    private val kontoId: UUID = UUID.fromString(props.fiks.io.kontoId)

    init {
        val oidcKeystore = KeyStore.getInstance(props.oidc.keystore.type)
        oidcKeystore.load(props.oidc.keystore.path.inputStream, props.oidc.keystore.password.toCharArray())

        val fiksIOConfig = FiksIOKonfigurasjon.builder()
                .amqpKonfigurasjon(AmqpKonfigurasjon.TEST)
                .fiksApiKonfigurasjon(FiksApiKonfigurasjon.TEST)
                .kontoKonfigurasjon(KontoKonfigurasjon.builder()
                        .kontoId(KontoId(kontoId))
                        .privatNokkel(ipNokkel.loadPrivateKey())
                        .build())
                .fiksIntegrasjonKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
                        .integrasjonId(UUID.fromString(props.fiks.io.integrasjonsId))
                        .integrasjonPassord(props.fiks.io.integrasjonsPassord)
                        .idPortenKonfigurasjon(IdPortenKonfigurasjon.VER2
                                .klientId(props.oidc.clientId)
                                .build())
                        .build())
                .virksomhetssertifikatKonfigurasjon(VirksomhetssertifikatKonfigurasjon.builder()
                        .keyStore(oidcKeystore)
                        .keyAlias(props.oidc.keystore.alias)
                        .keyPassword(props.oidc.keystore.password)
                        .keyStorePassword(props.oidc.keystore.password)
                        .build())
                .build()
        fiksIoKlient = FiksIOKlientFactory.build(fiksIOConfig)

        fiksIoKlient.newSubscription { mottattMelding, svarSender ->
            handleMessage(mottattMelding, svarSender)
        }
    }

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
        val request = MeldingRequest.builder()
                // TODO receive kontoId
                .mottakerKontoId(KontoId(kontoId))
                .meldingType(meldingType)
                .build()
        val sentMessage = fiksIoKlient.send(request, payloads)
        log.debug("FiksIO: Sent message with fiksId=${sentMessage.meldingId}")
    }

    private fun handleMessage(mottattMelding: MottattMelding, svarSender: SvarSender) {
        log.debug("FiksIO: Received message with fiksId=${mottattMelding.meldingId} messageType=${mottattMelding.meldingType}")
        mottattMelding.dekryptertZipStream.let {
            var entry = it.nextEntry
            while (entry != null) {
                log.debug("File name: ${entry.name}")
                log.debug("File content:")
                log.debug(String(it.readBytes()))
                if (entry.name == NextMoveConsts.SBD_FILE) {
                    val sbd = objectMapper.readValue(it.readBytes(), StandardBusinessDocument::class.java)
                }
                it.closeEntry()
                entry = it.nextEntry
            }
        }
        svarSender.ack()
    }

}