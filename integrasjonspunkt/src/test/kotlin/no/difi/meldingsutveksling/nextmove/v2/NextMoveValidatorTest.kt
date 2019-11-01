package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.exceptions.DuplicateFilenameException
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.receipt.ConversationService
import no.difi.meldingsutveksling.validation.Asserter
import org.junit.Before
import org.junit.Test

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
    }

    @Test(expected = DuplicateFilenameException::class)
    fun `duplicate filenames not allowed`() {
        val message = mockk<NextMoveOutMessage>()
        val bmf = BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true)
        every { message.orCreateFiles } returns mutableSetOf(bmf)

        val file = BasicNextMoveFile.of("title", "foo.txt", "text", "foo".toByteArray())
        nextMoveValidator.validateFile(message, file)
    }
}