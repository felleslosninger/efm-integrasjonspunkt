package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.*
import io.mockk.impl.annotations.MockK
import lombok.extern.slf4j.Slf4j
import no.difi.meldingsutveksling.MessageType
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.sbdh.SBDService
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.exceptions.*
import no.difi.meldingsutveksling.ks.svarut.SvarUtService
import no.difi.meldingsutveksling.nextmove.*
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.validation.Asserter
import no.difi.meldingsutveksling.validation.IntegrasjonspunktCertificateValidator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    lateinit var sbdService: SBDService

    @MockK
    lateinit var conversationService: ConversationService

    @MockK
    lateinit var arkivmeldingUtil: ArkivmeldingUtil

    @MockK
    lateinit var nextMoveFileSizeValidator: NextMoveFileSizeValidator

    @MockK
    lateinit var props: IntegrasjonspunktProperties

    @MockK
    lateinit var certValidator: ObjectProvider<IntegrasjonspunktCertificateValidator>

    @MockK
    lateinit var svarUtService: SvarUtService

    @MockK
    lateinit var svarUtServiceProvider: ObjectProvider<SvarUtService>

    private lateinit var nextMoveValidator: NextMoveValidator

    private val messageId = "123"
    private val message = mockk<NextMoveOutMessage>()
    private val sbd = mockk<StandardBusinessDocument>()
    private val serviceRecord = mockk<ServiceRecord>()
    private val businessMessage = ArkivmeldingMessage()
            .setHoveddokument("foo.txt")

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)
        nextMoveValidator = NextMoveValidator(
            serviceRecordProvider,
            nextMoveMessageOutRepository,
            conversationStrategyFactory,
            asserter,
            optionalCryptoMessagePersister,
            timeToLiveHelper,
            sbdService,
            conversationService,
            arkivmeldingUtil,
            nextMoveFileSizeValidator,
            props,
            certValidator,
            svarUtServiceProvider
        )


        val bmf = BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true)
        every { message.orCreateFiles } returns mutableSetOf(bmf)

        every { certValidator.ifAvailable(any()) } just Runs
        every { svarUtServiceProvider.getIfAvailable() } returns svarUtService
        every { svarUtServiceProvider.getObject() } returns svarUtService
        every { message.businessMessage } returns businessMessage

        every { sbd.messageId } returns messageId
        every { sbd.documentType } returns "standard::arkivmelding"
        every { sbd.process } returns "arkivmelding:administrasjon"

        every { message.messageId } returns messageId
        every { message.sbd } returns sbd
        every { message.serviceIdentifier } returns ServiceIdentifier.DPO
        every { message.files } returns emptySet()
        every { nextMoveMessageOutRepository.findByMessageId(messageId) } returns Optional.empty()
        every { conversationService.findConversation(messageId) } returns Optional.empty()
        every { serviceRecord.serviceIdentifier } returns ServiceIdentifier.DPO
        every { serviceRecordProvider.getServiceRecord(sbd) } returns serviceRecord
        every { conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO) } returns true

        mockkStatic(SBDUtil::class)
        every { SBDUtil.isStatus(sbd) } returns false
        every { SBDUtil.isReceipt(sbd) } returns false
        every { SBDUtil.isFileRequired(sbd) } returns true
        every { nextMoveFileSizeValidator.validate(any(), any()) } just Runs
    }

    @AfterEach
    fun after() {
        clearStaticMockk(SBDUtil::class)
    }

    @Test
    fun `message type must fit document type`() {
        every { sbd.documentType } returns "foo::bar"
        every { sbd.type } returns "arkivmelding"
        assertThrows(MessageTypeDoesNotFitDocumentTypeException::class.java) { nextMoveValidator.validate(sbd) }
    }

    @Test
    fun `document type must be valid`() {
        every { sbd.type } returns "melding"
        assertThrows(UnknownMessageTypeException::class.java) { nextMoveValidator.validate(sbd) }
    }

    @Test
    fun `service not enabled should throw exception`() {
        every { conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO) } returns false
        assertThrows(ServiceNotEnabledException::class.java) { nextMoveValidator.validate(sbd) }
    }

    @Test
    fun `duplicate messageId not allowed`() {
        every { nextMoveMessageOutRepository.findByMessageId(messageId) } returns Optional.of(message)
        assertThrows(MessageAlreadyExistsException::class.java) { nextMoveValidator.validate(sbd) }
    }

    @Test
    fun `conversation cannot exist with same messageId`() {
        every { conversationService.findConversation(messageId) } returns Optional.of(mockk<Conversation>())
        assertThrows(MessageAlreadyExistsException::class.java) { nextMoveValidator.validate(sbd) }
    }

    @Test
    fun `non-receipt messages must have attachments`() {
        every { message.files } returns null
        assertThrows(MissingFileException::class.java) { nextMoveValidator.validate(message) }
    }

    @Test
    fun `duplicate filenames not allowed`() {
        val file = BasicNextMoveFile.of("title", "foo.txt", "text", "foo".toByteArray())
        assertThrows(DuplicateFilenameException::class.java) { nextMoveValidator.validateFile(message, file) }
    }

    @Test
    fun `unknown document type allowed for fiksio message`() {
        every { sbd.documentType } returns "foo::bar"
        every { sbd.type } returns "fiksio"
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

    @Test
    fun `dpv message requires title`() {
        val filename = "bar.txt"
        every { message.serviceIdentifier } returns ServiceIdentifier.DPV
        every { message.isPrimaryDocument(filename) } returns false
        val file = BasicNextMoveFile.of("", "bar.txt", "text", "foo".toByteArray())
        assertThrows(MissingFileTitleException::class.java) { nextMoveValidator.validateFile(message, file) }
    }

    @Test
    fun `non-matching channel should throw exception`() {
        every { props.dpo.messageChannel } returns "foo-42"
        every { sbd.type } returns "arkivmelding"
        every { SBDUtil.getOptionalMessageChannel(sbd) } returns Optional.of(mockk {
            every { identifier } returns "foo-43"
        })
        assertThrows(MessageChannelInvalidException::class.java) { nextMoveValidator.validate(sbd) }
    }
}