package no.difi.meldingsutveksling.ks.fiksio

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.nextmove.NextMoveMessage
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
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

@Component
class FiksIoService(props: IntegrasjonspunktProperties,
                    ipNokkel: IntegrasjonspunktNokkel,
                    private val persister: OptionalCryptoMessagePersister,
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
            sendMessage(payloads = msg.files.map {
                StreamPayload(persister.readStream(msg.messageId, it.identifier, reject).inputStream, it.filename)
            }.toCollection(arrayListOf()))
        }

    }

    fun sendMessage(meldingType: String = "no.difi.einnsyn.innsynskrav.v1", payloads: List<Payload>) {
        val request = MeldingRequest.builder()
                // TODO receive kontoId
                .mottakerKontoId(KontoId(kontoId))
                .meldingType(meldingType)
                .build()
        fiksIoKlient.send(request, payloads)
    }

    private fun handleMessage(mottattMelding: MottattMelding, svarSender: SvarSender) {
        println("Mottatt melding med id=${mottattMelding.meldingId} meldingType=${mottattMelding.meldingType}")
        mottattMelding.dekryptertZipStream.let {
            var entry = it.nextEntry
            while (entry != null) {
                println("File name: ${entry.name}")
                println("File content:")
                println(String(it.readBytes()))
                it.closeEntry()
                entry = it.nextEntry
            }
        }
        svarSender.ack()
    }

}