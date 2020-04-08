package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.*
import io.mockk.impl.annotations.MockK
import lombok.extern.slf4j.Slf4j
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.exceptions.*
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.receipt.Conversation
import no.difi.meldingsutveksling.receipt.ConversationService
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.meldingsutveksling.validation.Asserter
import org.junit.Before
import org.junit.Test
import java.util.*

@Slf4j
class NextMoveValidatorTest {

    @MockK
    lateinit var nextMoveServiceRecordProvider: NextMoveServiceRecordProvider
    @MockK
    lateinit var nextMoveMessageOutRepository: NextMoveMessageOutRepository
    @MockK
    lateinit var serviceIdentifierService: ServiceIdentifierService
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
        nextMoveValidator = NextMoveValidator(nextMoveServiceRecordProvider,
                nextMoveMessageOutRepository,
                serviceIdentifierService,
                asserter,
                optionalCryptoMessagePersister,
                timeToLiveHelper,
                sbdUtil,
                conversationService,
                arkivmeldingUtil,
                nextMoveFileSizeValidator)

        val bmf = BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true)
        every { message.orCreateFiles } returns mutableSetOf(bmf)

        every { message.businessMessage } returns businessMessage
        every { sbd.optionalMessageId } returns Optional.of(messageId)
        every { message.messageId } returns messageId
        every { message.sbd } returns sbd
        every { message.serviceIdentifier } returns ServiceIdentifier.DPO
        every { nextMoveMessageOutRepository.findByMessageId(messageId) } returns Optional.empty()
        every { sbdUtil.isStatus(sbd) } returns false
        every { sbdUtil.isReceipt(sbd) } returns false
        every { conversationService.findConversation(messageId) } returns Optional.empty()
        every { serviceRecord.serviceIdentifier } returns ServiceIdentifier.DPO
        every { nextMoveServiceRecordProvider.getServiceRecord(sbd) } returns serviceRecord
        every { serviceIdentifierService.isEnabled(ServiceIdentifier.DPO) } returns true
        every { sbd.messageType } returns "arkivmelding"
        every { sbd.standard } returns "standard::arkivmelding"
        every { sbd.process } returns "arkivmelding:administrasjon"
        every { serviceRecord.hasStandard(any()) } returns true
        every { nextMoveFileSizeValidator.validate(any(), any()) } just Runs
    }

    @Test(expected = ReceiverDoNotAcceptDocumentStandard::class)
    fun `receiver must accept standard`() {
        every { serviceRecord.hasStandard(any()) } returns false
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = DocumentTypeDoNotFitDocumentStandardException::class)
    fun `standard must fit document type`() {
        every { sbd.standard } returns "foo::bar"
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = UnknownNextMoveDocumentTypeException::class)
    fun `document type must be valid`() {
        every { sbd.messageType } returns "foo"
        nextMoveValidator.validate(sbd)
    }

    @Test(expected = ServiceNotEnabledException::class)
    fun `service not enabled should throw exception`() {
        every { serviceIdentifierService.isEnabled(ServiceIdentifier.DPO) } returns false
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