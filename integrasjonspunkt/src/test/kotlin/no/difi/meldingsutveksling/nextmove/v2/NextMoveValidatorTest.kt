package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import lombok.extern.slf4j.Slf4j
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.exceptions.DuplicateFilenameException
import no.difi.meldingsutveksling.exceptions.MissingFileTitleException
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.receipt.ConversationService
import no.difi.meldingsutveksling.validation.Asserter
import org.junit.Before
import org.junit.Test

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

    lateinit var nextMoveValidator : NextMoveValidator

    val message = mockk<NextMoveOutMessage>()

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
                arkivmeldingUtil)

        val bmf = BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true)
        every { message.orCreateFiles } returns mutableSetOf(bmf)

        val businessMessage = ArkivmeldingMessage()
                .setHoveddokument("foo.txt")
        every { message.businessMessage } returns businessMessage
    }

    @Test
    fun `non-receipt messages must have attachments`() {
        val sbd = mockk<StandardBusinessDocument>()
        every { message.sbd } returns sbd
        every { sbdUtil.isReceipt(sbd) } returns false

    }

    @Test(expected = DuplicateFilenameException::class)
    fun `duplicate filenames not allowed`() {
        val file = BasicNextMoveFile.of("title", "foo.txt", "text", "foo".toByteArray())
        nextMoveValidator.validateFile(message, file)
    }

    @Test
    fun `dpo message does not require title`() {
        val filename = "bar.txt"
        every { message.serviceIdentifier } returns ServiceIdentifier.DPO
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