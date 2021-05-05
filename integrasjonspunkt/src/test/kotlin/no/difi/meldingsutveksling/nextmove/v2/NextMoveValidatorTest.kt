package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.*
import io.mockk.impl.annotations.MockK
import lombok.extern.slf4j.Slf4j
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.ScopeType
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.exceptions.*
import no.difi.meldingsutveksling.nextmove.*
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.validation.Asserter
import no.difi.meldingsutveksling.validation.IntegrasjonspunktCertificateValidator
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.ObjectProvider
import java.util.*

@Slf4j
class NextMoveValidatorTest {

    @MockK
    lateinit var serviceRecordProvider: ServiceRecordProvider
    @MockK
    lateinit var nextMoveMessageOutRepository: NextMoveMessageOutRepository
    @MockK
    lateinit var conversationStrategyFactory: ConversationStrategyFactory
    @MockK
    lateinit var asserter: Asserter
    @MockK
    lateinit var optionalCryptoMessagePersister: OptionalCryptoMessagePersister
    @MockK
    lateinit var timeToLiveHelper: TimeToLiveHelper
    @MockK
    lateinit var sbdUtil: SBDUtil
    @MockK
    lateinit var conversationService : ConversationService
    @MockK
    lateinit var arkivmeldingUtil: ArkivmeldingUtil
    @MockK
    lateinit var nextMoveFileSizeValidator: NextMoveFileSizeValidator
    @MockK
    lateinit var certValidator: ObjectProvider<IntegrasjonspunktCertificateValidator>

    private lateinit var nextMoveValidator : NextMoveValidator

    val messageId = "123"
    val message = mockk<NextMoveOutMessage>()
    val sbd = mockk<StandardBusinessDocument>()
    val serviceRecord = mockk<ServiceRecord>()
    val businessMessage = ArkivmeldingMessage()
            .setHoveddokument("foo.txt")

    @Before
    fun before() {
        MockKAnnotations.init(this)
        nextMoveValidator = NextMoveValidator(serviceRecordProvider,
            nextMoveMessageOutRepository,
            conversationStrategyFactory,
            asserter,
            optionalCryptoMessagePersister,
            timeToLiveHelper,
            sbdUtil,
            conversationService,
            arkivmeldingUtil,
            nextMoveFileSizeValidator,
            certValidator)


        val bmf = BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true)
        every { message.orCreateFiles } returns mutableSetOf(bmf)

        every { certValidator.ifAvailable(any()) } just Runs
        every { message.businessMessage } returns businessMessage
        every { sbd.optionalMessageId } returns Optional.of(messageId)
        every { message.messageId } returns messageId
        every { message.sbd } returns sbd
        every { message.serviceIdentifier } returns ServiceIdentifier.DPO
        every { message.files } returns emptySet()
        every { nextMoveMessageOutRepository.findByMessageId(messageId) } returns Optional.empty()
        every { sbdUtil.isStatus(sbd) } returns false
        every { sbdUtil.isReceipt(sbd) } returns false
        every { sbdUtil.isFileRequired(sbd) } returns true
        every { conversationService.findConversation(messageId) } returns Optional.empty()
        every { serviceRecord.serviceIdentifier } returns ServiceIdentifier.DPO
        every { serviceRecordProvider.getServiceRecord(sbd) } returns serviceRecord
        every { conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO) } returns true
        every { sbd.messageType } returns "arkivmelding"
        every { sbd.documentType } returns "standard::arkivmelding"
        every { sbd.process } returns "arkivmelding:administrasjon"
        every { sbd.optionalConversationId } returns Optional.of(UUID.randomUUID().toString())
        every { sbd.findScope(ScopeType.SENDER_REF) } returns Optional.empty()
        every { nextMoveFileSizeValidator.validate(any(), any()) } just Runs
    }

    @Test(expected = MessageTypeDoesNotFitDocumentTypeException::class)
    fun `message type must fit document type`() {
        every { sbd.documentType } returns "foo::bar"
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = UnknownMessageTypeException::class)
    fun `document type must be valid`() {
        every { sbd.messageType } returns "foo"
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = ServiceNotEnabledException::class)
    fun `service not enabled should throw exception`() {
        every { conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO) } returns false
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = MessageAlreadyExistsException::class)
    fun `duplicate messageId not allowed`() {
        every { nextMoveMessageOutRepository.findByMessageId(messageId) } returns Optional.of(message)
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = MessageAlreadyExistsException::class)
    fun `conversation cannot exist with same messageId`() {
        every { conversationService.findConversation(messageId) } returns Optional.of(mockk<Conversation>())
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = MissingFileException::class)
    fun `non-receipt messages must have attachments`() {
        every { message.files } returns null
        nextMoveValidator.validate(message)
    }

    @Test(expected = DuplicateFilenameException::class)
    fun `duplicate filenames not allowed`() {
        val file = BasicNextMoveFile.of("title", "foo.txt", "text", "foo".toByteArray())
        nextMoveValidator.validateFile(message, file)
    }

    @Test
    fun `unknown document type allowed for fiksio message`() {
        every { sbd.documentType } returns "foo::bar"
        every { serviceRecord.serviceIdentifier } returns ServiceIdentifier.DPFIO
        every { conversationStrategyFactory.isEnabled(ServiceIdentifier.DPFIO) } returns true
        every { sbd.any } returns mockkClass(FiksIoMessage::class)
        every { asserter.isValid(any<FiksIoMessage>(), any()) } just Runs

        nextMoveValidator.validate(sbd)
    }


    @Test
    fun `dpo message does not require title`() {
        val filename = "bar.txt"
        every { message.isPrimaryDocument(filename) } returns false
       val file = BasicNextMoveFile.of("", filename, "text", "foo".toByteArray())
        nextMoveValidator.validateFile(message, file)
    }

    @Test(expected = MissingFileTitleException::class)
    fun `dpv message requires title`() {
        val filename = "bar.txt"
        every { message.serviceIdentifier } returns ServiceIdentifier.DPV
        every { message.isPrimaryDocument(filename) } returns false
        val file = BasicNextMoveFile.of("", "bar.txt", "text", "foo".toByteArray())
        nextMoveValidator.validateFile(message, file)
    }
}