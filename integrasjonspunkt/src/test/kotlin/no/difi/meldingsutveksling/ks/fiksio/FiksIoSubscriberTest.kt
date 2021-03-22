package no.difi.meldingsutveksling.ks.fiksio

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.dokumentpakking.service.SBDFactory
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

class FiksIoSubscriberTest {

    @MockK
    lateinit var fiksIOKlient: FiksIOKlient
    @MockK
    lateinit var nextMoveQueue: NextMoveQueue
    @MockK
    lateinit var sbdFactory: SBDFactory
    @MockK
    lateinit var props: IntegrasjonspunktProperties

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
        zos.write(sbdBytes)
        zos.closeEntry()
        zos.close()
        zipInputStream = ZipInputStream(bos.toByteArray().inputStream())

        every { fiksIOKlient.newSubscription(any()) } just Runs
        every { props.org.number } returns "123123123"
        every { props.fiks.io.senderOrgnr } returns "321321321"
        every { sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(),  any(), any()) } returns mockkClass(StandardBusinessDocument::class)
        every { nextMoveQueue.enqueueIncomingMessage(any(), any(), any()) } just Runs

        fiksIoSubscriber = FiksIoSubscriber(fiksIOKlient, sbdFactory, props, nextMoveQueue)
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

        verify { nextMoveQueue.enqueueIncomingMessage(any(), ServiceIdentifier.DPFIO, any()) }
        verify { svarSender.ack() }
    }

}