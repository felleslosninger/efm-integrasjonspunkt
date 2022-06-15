package no.difi.meldingsutveksling.ks.fiksio

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.sbd.SBDFactory
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.SvarSender
import no.ks.fiks.io.client.model.MeldingId
import no.ks.fiks.io.client.model.MottattMelding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.util.*

class FiksIoSubscriberTest {

    @MockK
    lateinit var fiksIOKlient: FiksIOKlient
    @MockK
    lateinit var nextMoveQueue: NextMoveQueue
    @MockK
    lateinit var sbdFactory: SBDFactory
    @MockK
    lateinit var props: IntegrasjonspunktProperties

    lateinit var sbd: StandardBusinessDocument
    lateinit var fiksIoSubscriber: FiksIoSubscriber

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)

        every { fiksIOKlient.newSubscription(any()) } just Runs
        every { props.org.identifier } returns "123123123"
        every { props.fiks.io.senderOrgnr } returns "321321321"
        every { sbdFactory.createNextMoveSBD(any(), any(), any(), any(), any(),  any(), any(), any()) } returns mockkClass(StandardBusinessDocument::class)
        every { nextMoveQueue.enqueueIncomingMessage(any(), any(), any()) } just Runs

        fiksIoSubscriber = FiksIoSubscriber(fiksIOKlient, sbdFactory, props, nextMoveQueue)
    }

    @Test
    fun `test handle message`() {
        val mottattMelding = mockkClass(MottattMelding::class) {
            every { meldingId } returns MeldingId(UUID.fromString("6d16a689-da59-4d22-8e3e-82bcb9169ccb"))
            every { meldingType } returns "no.digdir.einnsyn.v1"
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