package no.difi.meldingsutveksling.nextmove.v2

import no.difi.meldingsutveksling.ServiceIdentifier.*
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.exceptions.MaxFileSizeExceededException
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile

@Component
class NextMoveFileSizeValidator(private val props: IntegrasjonspunktProperties) {

    fun validate(msg: NextMoveOutMessage, file: MultipartFile) {
        val total = DataSize.ofBytes(msg.files.map { it.size }.fold(file.size) { a, b -> a + b })
        val limit = when (msg.serviceIdentifier) {
            DPO -> props.dpo.uploadSizeLimit
            DPV -> props.dpv.uploadSizeLimit
            DPE -> props.nextmove.serviceBus.uploadSizeLimit
            DPF -> props.fiks.ut.uploadSizeLimit
            DPI -> props.dpi.uploadSizeLimit
            DPFIO -> props.fiks.io.uploadSizeLimit
            DPH -> props.dph.uploadSizeLimit
            else -> throw NextMoveRuntimeException("Unknown Service Identifier")
        }
        if (total > limit) {
            if (total < DataSize.ofMegabytes(1L)) {
                throw MaxFileSizeExceededException("${total.toBytes()}b", msg.serviceIdentifier.toString(), "${limit.toBytes()}b")
            }
            throw MaxFileSizeExceededException("${total.toMegabytes()}MB", msg.serviceIdentifier.toString(), "${limit.toMegabytes()}MB")
        }
    }
}

