package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest
import no.difi.meldingsutveksling.nextmove.v2.CryptoMessageResource
import no.difi.meldingsutveksling.pipes.Reject
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.digdir.dpi.client.domain.Document
import no.digdir.dpi.client.domain.MetadataDocument
import org.springframework.core.io.Resource
import java.time.Clock
import java.util.*

class NextMoveDpiRequest(
    private val props: IntegrasjonspunktProperties,
    private val clock: Clock,
    private val optionalCryptoMessagePersister: OptionalCryptoMessagePersister,
    private val reject: Reject
) {
    private fun getMeldingsformidlerRequest(message: NextMoveMessage, serviceRecord: ServiceRecord) : MeldingsformidlerRequest {
        return new
    }

    private fun createDocument(file: BusinessMessageFile): Document {
        val title = if (file.title.isNullOrBlank()) "Missing title" else file.title
        val document = Document()
            .setResource(getContent(file.identifier))
            .setMimeType(file.mimetype)
            .setFilename(file.filename)
            .setTitle(title)

        if (isDigitalMessage && getDigitalMessage().metadataFiler.containsKey(file.filename)) {
            val metadataFilename = getDigitalMessage().metadataFiler[file.filename]
            val metadataFile = message.files.first { it.filename == metadataFilename }
                ?: throw NextMoveRuntimeException("Metadata document $metadataFilename specified for ${file.filename}, but is not attached")
            document.metadataDocument = MetadataDocument()
                .setFilename(metadataFilename)
                .setMimeType(metadataFile.mimetype)
                .setResource(getContent(metadataFile.identifier))
        }

        return document
    }

    private fun getContent(fileName: String): Resource {
        return CryptoMessageResource(message.messageId, fileName, optionalCryptoMessagePersister, reject)
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

    override fun getStandardBusinessDocumentHeader(): StandardBusinessDocumentHeader {
        return message.sbd.standardBusinessDocumentHeader
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
        return SBDUtil.getOnBehalfOfOrgNr(message.sbd)
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

    override fun getSecurityLevel(): Int? {
        return if (message.businessMessage is DpiDigitalMessage) {
            return message.businessMessage.sikkerhetsnivaa
        } else null
    }

    override fun getVirkningsdato(): Date {
        return if (isDigitalMessage) {
            Date.from(
                getDigitalMessage().digitalPostInfo
                    .virkningsdato
                    .atStartOfDay()
                    .atZone(clock.zone)
                    .toInstant()
            )
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

    override fun getPrintColor(): PrintColor {
        return if (isPrintMessage) {
            getPrintMessage().utskriftsfarge
        } else PrintColor.SORT_HVIT
    }

    override fun getPostalCategory(): PostalCategory {
        return if (isPrintMessage) {
            getPrintMessage().posttype
        } else PostalCategory.B_OEKONOMI
    }

    override fun getReturnHandling(): ReturnHandling {
        return if (isPrintMessage) {
            getPrintMessage().retur.returhaandtering
        } else ReturnHandling.DIREKTE_RETUR
    }
}