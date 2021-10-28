package no.difi.meldingsutveksling.dokumentpakking.service

import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.DateTimeUtil
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.Organisasjonsnummer
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentUtils
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage
import no.difi.meldingsutveksling.nextmove.StatusMessage
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SbdFactoryTest {

    @MockK
    lateinit var serviceRegistryLookup: ServiceRegistryLookup

    @MockK
    lateinit var props: IntegrasjonspunktProperties

    val clock: Clock = Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DateTimeUtil.DEFAULT_ZONE_ID)
    private lateinit var sbdFactory: SBDFactory

    private val arkivmeldingProcess = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0"
    private val arkivmeldingResponseProcess = "urn:no:difi:profile:arkivmelding:response:ver1.0"
    private val einnsynResponseProcess = "urn:no:difi:profile:einnsyn:response:ver1.0"
    private val statusDocType = "urn:no:difi:eformidling:xsd::status"
    private val senderOrgnr = "910076787"
    private val receiverOrgnr = "991825827"
    private val convId = "e3016cb7-39de-4166-a935-3a574cd2a2db"
    private val msgId = "4653f436-8921-4224-b824-068f2cc6232f"

    val sbd: StandardBusinessDocument = mockk {
//        every { receiver } returns Organisasjonsnummer.from(receiverOrgnr)
//        every { sender } returns Organisasjonsnummer.from(senderOrgnr)
//        every { senderIdentifier } returns senderOrgnr
//        every { receiverIdentifier } returns receiverOrgnr
//        every { conversationId } returns convId
//        every { messageId } returns msgId
    }

    @Before
    fun before() {
        MockKAnnotations.init(this)
        sbdFactory = SBDFactory(serviceRegistryLookup, clock, props)

        every { props.arkivmelding } returns mockk {
            every { receiptProcess } returns arkivmeldingResponseProcess
        }
        every { props.einnsyn } returns mockk {
            every { receiptProcess } returns einnsynResponseProcess
        }
        every { props.nextmove } returns mockk {
            every { statusDocumentType } returns statusDocType
            every { defaultTtlHours } returns 24
        }

        mockkStatic(SBDUtil::class)
        every { SBDUtil.getReceiver(sbd) } returns Organisasjonsnummer.from(receiverOrgnr)
        every { SBDUtil.getSender(sbd) } returns Organisasjonsnummer.from(senderOrgnr)
        every { SBDUtil.getSenderIdentifier(sbd) } returns senderOrgnr
        every { SBDUtil.getReceiverIdentifier(sbd) } returns receiverOrgnr
        every { SBDUtil.getConversationId(sbd) } returns convId
        every {  SBDUtil.getMessageId(sbd) } returns msgId
    }

    @After
    fun after() {
        clearStaticMockk(SBDUtil::class)
    }

    @Test
    fun `test status creation from arkivmelding message`() {
        every { SBDUtil.isArkivmelding(sbd) } returns true

        val statusSbd = sbdFactory.createStatusFrom(sbd, ReceiptStatus.LEVERT)

        assertEquals(StandardBusinessDocumentUtils.getFirstReceiver(statusSbd).get().identifier.value,
            "0192:$senderOrgnr"
        )
        assertEquals(StandardBusinessDocumentUtils.getFirstSender(statusSbd).get().identifier.value,
            "0192:$receiverOrgnr"
        )
        assertEquals(arkivmeldingResponseProcess, SBDUtil.getProcess(statusSbd))
        assertTrue(statusSbd.any is StatusMessage)
        assertEquals(ReceiptStatus.LEVERT, (statusSbd.any as StatusMessage).status)
    }

    @Test
    fun `test status creation from einnsyn message`() {
        every { SBDUtil.isArkivmelding(sbd) } returns false
        every { SBDUtil.isEinnsyn(sbd) } returns true

        val statusSbd = sbdFactory.createStatusFrom(sbd, ReceiptStatus.LEVERT)
        assertEquals(einnsynResponseProcess, SBDUtil.getProcess(statusSbd))
    }

    @Test(expected = MeldingsUtvekslingRuntimeException::class)
    fun `test message type validation`() {
        every { serviceRegistryLookup.getServiceRecord(any(), any()) } returns mockk {
            every { serviceIdentifier } returns ServiceIdentifier.DPO
            every { documentTypes } returns listOf("foo::bar")
        }

        sbdFactory.createNextMoveSBD(
            Organisasjonsnummer.from(senderOrgnr),
            Organisasjonsnummer.from(receiverOrgnr),
            convId, msgId,
            arkivmeldingProcess,
            "foo::bar",
            mockkClass(ArkivmeldingMessage::class)
        )
    }

    @Test
    fun `unknown document type allowed for fiksio message type`() {
        every { serviceRegistryLookup.getServiceRecord(any(), any()) } returns mockk {
            every { serviceIdentifier } returns ServiceIdentifier.DPFIO
            every { documentTypes } returns listOf("foo::bar")
        }

        sbdFactory.createNextMoveSBD(
            Organisasjonsnummer.from(senderOrgnr),
            Organisasjonsnummer.from(receiverOrgnr),
            convId, msgId,
            arkivmeldingProcess,
            "foo::bar",
            mockkClass(ArkivmeldingMessage::class)
        )
    }
}