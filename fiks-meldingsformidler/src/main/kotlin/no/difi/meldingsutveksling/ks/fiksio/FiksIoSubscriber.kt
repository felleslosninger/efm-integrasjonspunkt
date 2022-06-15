package no.difi.meldingsutveksling.ks.fiksio

import no.difi.meldingsutveksling.MessageType
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.DpfioPolling
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.ICD
import no.difi.meldingsutveksling.domain.Iso6523
import no.difi.meldingsutveksling.sbd.SBDFactory
import no.difi.meldingsutveksling.nextmove.FiksIoMessage
import no.difi.meldingsutveksling.util.logger
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.model.MottattMelding
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class FiksIoSubscriber(private val fiksIOKlient: FiksIOKlient,
                       private val sbdFactory: SBDFactory,
                       private val props: IntegrasjonspunktProperties,
                       private val nextMoveQueue: NextMoveQueue): DpfioPolling {

    val log = logger()

    @PostConstruct
    fun registerSubscriber() {
        if (props.fiks.io.senderOrgnr.isNullOrBlank()) throw IllegalArgumentException("difi.move.fiks.io.sender-orgnr must not be null")
        fiksIOKlient.newSubscription { mottattMelding, svarSender ->
            handleMessage(mottattMelding, svarSender)
        }
    }

    private fun handleMessage(mottattMelding: MottattMelding, svarSender: SvarSender) {
        log.debug("FiksIO: Received message with fiksId=${mottattMelding.meldingId} protocol=${mottattMelding.meldingType}")
        val sbd = sbdFactory.createNextMoveSBD(
            Iso6523.of(ICD.NO_ORG, props.fiks.io.senderOrgnr),
            Iso6523.of(ICD.NO_ORG, props.org.identifier),
            mottattMelding.meldingId.toString(),
            mottattMelding.meldingId.toString(),
            mottattMelding.meldingType,
            mottattMelding.meldingType,
            MessageType.FIKSIO,
            FiksIoMessage()
        )
        nextMoveQueue.enqueueIncomingMessage(sbd, ServiceIdentifier.DPFIO, mottattMelding.kryptertStream)
        svarSender.ack()
    }

    override fun poll() {
        // noop
    }
}