package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.databind.ObjectMapper
import no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE
import no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE
import no.difi.meldingsutveksling.ServiceIdentifier.DPF
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.NextMoveQueue
import no.difi.meldingsutveksling.nextmove.message.MessagePersister
import no.difi.meldingsutveksling.util.logger
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.model.MottattMelding
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPFIO"], havingValue = "true")
class FiksIoSubscriber(fiksIOKlient: FiksIOKlient,
                       private val objectMapper: ObjectMapper,
                       private val nextMoveQueue: NextMoveQueue,
                       private val persister: MessagePersister) {

    val log = logger()

    init {
        fiksIOKlient.newSubscription { mottattMelding, svarSender ->
            handleMessage(mottattMelding, svarSender)
        }
    }

    private fun handleMessage(mottattMelding: MottattMelding, svarSender: SvarSender) {
        log.debug("FiksIO: Received message with fiksId=${mottattMelding.meldingId} messageType=${mottattMelding.meldingType}")
        mottattMelding.dekryptertZipStream.let {
            var entry = it.nextEntry
            var sbd: StandardBusinessDocument? = null
            while (entry != null) {
                log.debug("File name: ${entry.name}")
                if (entry.name == SBD_FILE) {
                    try {
                        sbd = objectMapper.readValue(it.readBytes(), StandardBusinessDocument::class.java)
                    } catch (e: IOException) {
                        log.error("Error reading ${StandardBusinessDocument::class.simpleName} from \'${SBD_FILE}\'")
                    }
                }
                it.closeEntry()
                entry = it.nextEntry
            }
            if (sbd != null) {
                persister.writeStream(sbd.messageId, ASIC_FILE, mottattMelding.kryptertStream, -1)
                nextMoveQueue.enqueue(sbd, DPF)
            } else {
                log.error("Missing file \'${SBD_FILE}\' from Fiks IO message with id=${mottattMelding.meldingId}")
            }
        }
        svarSender.ack()
    }
}