package no.difi.meldingsutveksling.nextmove.v2

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.exceptions.MaxFileSizeExceededException
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import jakarta.servlet.http.HttpServletRequest

class NextMoveFileSizeValidatorTest {

    @MockK
    lateinit var props: IntegrasjonspunktProperties

    private lateinit var validator: NextMoveFileSizeValidator
    private val msg = mockk<NextMoveOutMessage>()
    private val req = mockk<HttpServletRequest>()
    val file = NextMoveUploadedFile("text/html", "attachment; filename=\"test.txt\"", "title", req)

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)
        every { props.dpo.uploadSizeLimit } returns DataSize.parse("10MB")
        validator = NextMoveFileSizeValidator(props)

        every { msg.serviceIdentifier } returns ServiceIdentifier.DPO
        every { msg.files } returns emptySet()
    }

    @Test
    fun `test upload is within limit`() {
        every { req.contentLengthLong } returns DataSize.parse("5MB").toBytes()
        validator.validate(msg, file)
    }

    @Test
    fun `test multiple uploads is within limit size`() {
        val existingFile = mockk<BusinessMessageFile>()
        every { existingFile.size } returns DataSize.parse("4MB").toBytes()
        every { msg.files } returns setOf(existingFile)
        every { req.contentLengthLong } returns DataSize.parse("5MB").toBytes()
        validator.validate(msg, file)
    }

    @Test
    fun `test upload exceeds limit size`() {
        every { req.contentLengthLong } returns DataSize.parse("100MB").toBytes()
        assertThrows(MaxFileSizeExceededException::class.java) { validator.validate(msg, file) }
    }

    @Test
    fun `test multiple uploads exceed limit size`() {
        val existingFile = mockk<BusinessMessageFile>()
        every { existingFile.size } returns DataSize.parse("6MB").toBytes()
        every { msg.files } returns setOf(existingFile)
        every { req.contentLengthLong } returns DataSize.parse("5MB").toBytes()
        assertThrows(MaxFileSizeExceededException::class.java) { validator.validate(msg, file) }
    }

}