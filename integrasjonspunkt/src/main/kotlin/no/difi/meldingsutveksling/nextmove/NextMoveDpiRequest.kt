package no.difi.meldingsutveksling.nextmove

import no.difi.begrep.sdp.schema_v10.SDPSikkerhetsnivaa
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.dpi.Document
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.sdp.client2.domain.MetadataDokument
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa
import no.difi.sdp.client2.domain.fysisk_post.Posttype
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge
import java.time.Clock
import java.util.*

class NextMoveDpiRequest(private val props: IntegrasjonspunktProperties,
                         private val clock: Clock,
                         private val message: NextMoveMessage,
                         private val serviceRecord: ServiceRecord,
                         private val optionalCryptoMessagePersister: OptionalCryptoMessagePersister) : MeldingsformidlerRequest {

    private fun createDocument(file: BusinessMessageFile): Document {
        val title = if (file.title.isNullOrBlank()) "Missing title" else file.title
        val document = Document(getContent(file.identifier), file.mimetype, file.filename, title)

        if (isDigitalMessage && getDigitalMessage().metadataFiler.containsKey(file.filename)) {
            val metadataFilename = getDigitalMessage().metadataFiler[file.filename]
            val metadataFile = message.files.first { it.filename == metadataFilename }
                    ?: throw NextMoveRuntimeException("Metadata document $metadataFilename specified for ${file.filename}, but is not attached")
            document.metadataDokument = MetadataDokument(metadataFilename, metadataFile.mimetype, getContent(metadataFile.identifier))
        }

        return document
    }

    private fun getContent(fileName: String): ByteArray {
        return optionalCryptoMessagePersister.read(message.messageId, fileName)
    }

    private fun isMetadataFile(filename: String): Boolean {
        return if (isPrintMessage) {
            false
        } else getDigitalMessage().metadataFiler.containsValue(filename)
    }

    private val isDigitalMessage: Boolean
        get() = message.businessMessage is DpiDigitalMessage

    private val isPrintMessage: Boolean
        get() = message.businessMessage is DpiPrintMessage

    private fun getDigitalMessage(): DpiDigitalMessage {
        return message.businessMessage as DpiDigitalMessage
    }

    private fun getPrintMessage(): DpiPrintMessage {
        return message.businessMessage as DpiPrintMessage
    }

    override fun getDocument(): Document {
        return message.files.first { it.primaryDocument }
                ?.let { createDocument(it) }
                ?: throw NextMoveRuntimeException("No primary documents found, aborting send")
    }

    override fun getAttachments(): List<Document> {
        return message.files.filter { !it.primaryDocument }
                .filter { !isMetadataFile(it.filename) }
                .map { createDocument(it) }
    }

    override fun getMottakerPid(): String {
        return message.receiverIdentifier
    }

    override fun getSubject(): String? {
        return when (val m = message.businessMessage) {
            is DpiDigitalMessage -> m.tittel
            is DpiPrintMessage -> null
            else -> throw NextMoveRuntimeException("BusinessMessage not instance of either ${DpiDigitalMessage::class.simpleName} or ${DpiPrintMessage::class.simpleName}")
        }
    }

    override fun getSenderOrgnumber(): String {
        return message.senderIdentifier
    }

    override fun getOnBehalfOfOrgnr(): Optional<String> {
        return message.sbd.partIdentifier
    }

    override fun getAvsenderIdentifikator(): Optional<String> {
        return when (val m = message.businessMessage) {
            is DpiDigitalMessage -> Optional.ofNullable(m.avsenderId)
            is DpiPrintMessage -> Optional.ofNullable(m.avsenderId)
            else -> Optional.empty()
        }
    }

    override fun getFakturaReferanse(): Optional<String> {
        return when (val m = message.businessMessage) {
            is DpiDigitalMessage -> Optional.ofNullable(m.fakturaReferanse)
            is DpiPrintMessage -> Optional.ofNullable(m.fakturaReferanse)
            else -> Optional.empty()
        }
    }

    override fun getConversationId(): String {
        return message.messageId
    }

    override fun getPostkasseAdresse(): String {
        return serviceRecord.postkasseAdresse
    }

    override fun getCertificate(): ByteArray {
        return serviceRecord.pemCertificate.toByteArray()
    }

    override fun getOrgnrPostkasse(): String {
        return serviceRecord.orgnrPostkasse
    }

    override fun getEmailAddress(): String? {
        return serviceRecord.epostAdresse
    }

    override fun getSmsVarslingstekst(): String? {
        return if (isDigitalMessage) getDigitalMessage().varsler?.smsTekst else null
    }

    override fun getEmailVarslingstekst(): String? {
        return if (isDigitalMessage) getDigitalMessage().varsler?.epostTekst else null
    }

    override fun getMobileNumber(): String? {
        return serviceRecord.mobilnummer
    }

    override fun isNotifiable(): Boolean {
        return serviceRecord.isKanVarsles
    }

    override fun isPrintProvider(): Boolean {
        return isPrintMessage
    }

    override fun getPostAddress(): PostAddress? {
        return if (isPrintMessage) {
            getPrintMessage().mottaker
        } else null
    }

    override fun getReturnAddress(): PostAddress? {
        return if (isPrintMessage) {
            getPrintMessage().retur.mottaker
        } else null
    }

    override fun getSecurityLevel(): Sikkerhetsnivaa? {
        return if (message.businessMessage is DpiDigitalMessage) {
            return Sikkerhetsnivaa.valueOf(SDPSikkerhetsnivaa.fromValue(message.businessMessage.getSikkerhetsnivaa().toString()).toString())
        } else null
    }

    override fun getVirkningsdato(): Date {
        return if (isDigitalMessage) {
            Date.from(getDigitalMessage().digitalPostInfo
                    .virkningsdato
                    .atStartOfDay()
                    .atZone(clock.zone)
                    .toInstant())
        } else Date()
    }

    override fun getLanguage(): String {
        return if (isDigitalMessage) {
            getDigitalMessage().spraak
        } else props.dpi.language
    }

    override fun isAapningskvittering(): Boolean {
        return if (isDigitalMessage) {
            getDigitalMessage().digitalPostInfo.aapningskvittering
        } else false
    }

    override fun getPrintColor(): Utskriftsfarge {
        return if (isPrintMessage) {
            getPrintMessage().utskriftsfarge
        } else Utskriftsfarge.SORT_HVIT
    }

    override fun getPosttype(): Posttype {
        return if (isPrintMessage) {
            getPrintMessage().posttype
        } else Posttype.B_OEKONOMI
    }

    override fun getReturnHandling(): Returhaandtering {
        return if (isPrintMessage) {
            getPrintMessage().retur.returhaandtering
        } else Returhaandtering.DIREKTE_RETUR
    }

}