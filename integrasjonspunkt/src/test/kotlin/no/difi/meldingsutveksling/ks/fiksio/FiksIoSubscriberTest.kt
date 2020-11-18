package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.model.MeldingId
import no.ks.fiks.io.client.model.MottattMelding
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals

class FiksIoSubscriberTest {

    @MockK
    lateinit var fiksIOKlient: FiksIOKlient

    @MockK
    lateinit var objectMapper: ObjectMapper

    @MockK
    lateinit var nextMoveQueue: NextMoveQueue

    lateinit var sbdCapture: CapturingSlot<StandardBusinessDocument>
    lateinit var zipInputStream: ZipInputStream
    lateinit var sbd: StandardBusinessDocument
    lateinit var fiksIoSubscriber: FiksIoSubscriber

    @Before
    fun before() {
        MockKAnnotations.init(this)

        val sbdStream = this.javaClass.classLoader.getResourceAsStream("sbd/StandardBusinessDocument.json")
                ?: throw RuntimeException("sbd test file not found")
        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)
        zos.putNextEntry(ZipEntry(SBD_FILE))
        val sbdBytes = sbdStream.readBytes()
        sbd = ObjectMapper().registerModule(JavaTimeModule()).readValue(sbdBytes, StandardBusinessDocument::class.java)
        zos.write(sbdBytes)
        zos.closeEntry()
        zos.close()
        zipInputStream = ZipInputStream(bos.toByteArray().inputStream())

        sbdCapture = slot()

        every { fiksIOKlient.newSubscription(any()) } just Runs
        every { objectMapper.readValue(any<ByteArray>(), StandardBusinessDocument::class.java) } returns sbd
        every { nextMoveQueue.enqueueIncomingMessage(capture(sbdCapture), ServiceIdentifier.DPFIO, any()) } just Runs

        fiksIoSubscriber = FiksIoSubscriber(fiksIOKlient, objectMapper, nextMoveQueue)
    }

    @Test
    fun `test handle message`() {
        val mottattMelding = mockkClass(MottattMelding::class) {
            every { meldingId } returns MeldingId(UUID.fromString("6d16a689-da59-4d22-8e3e-82bcb9169ccb"))
            every { meldingType } returns "no.digdir.einnsyn.v1"

            every { dekryptertZipStream } returns zipInputStream
            every { kryptertStream } returns mockkClass(InputStream::class)
        }

        val svarSender = mockkClass(SvarSender::class, relaxed = true)

        val m = fiksIoSubscriber.javaClass.getDeclaredMethod("handleMessage", MottattMelding::class.java, SvarSender::class.java)
        m.isAccessible = true
        m.invoke(fiksIoSubscriber, mottattMelding, svarSender)

        assertEquals("ff88849c-e281-4809-8555-7cd54952b916", sbdCapture.captured.messageId)
        verify { nextMoveQueue.enqueueIncomingMessage(any(), ServiceIdentifier.DPFIO, any()) }
        verify { svarSender.ack() }
    }

}