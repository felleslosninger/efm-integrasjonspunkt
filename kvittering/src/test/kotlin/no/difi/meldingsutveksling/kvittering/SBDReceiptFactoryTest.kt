package no.difi.meldingsutveksling.kvittering

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import no.difi.meldingsutveksling.DocumentType
import no.difi.meldingsutveksling.Process
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.StatusMessage
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SBDReceiptFactoryTest {

    @MockK
    lateinit var serviceRegistryLookup: ServiceRegistryLookup
    @MockK
    lateinit var sbdUtil: SBDUtil

    private lateinit var sbdReceiptFactory: SBDReceiptFactory

    private val status = "urn:no:difi:eformidling:xsd::status"
    private val senderOrgnr = "910076787"
    private val receiverOrgnr = "991825827"
    private val convId = "e3016cb7-39de-4166-a935-3a574cd2a2db"
    private val msgId = "4653f436-8921-4224-b824-068f2cc6232f"

    val sbd: StandardBusinessDocument = mockk {
        every { senderIdentifier } returns senderOrgnr
        every { receiverIdentifier } returns receiverOrgnr
        every { conversationId } returns convId
        every { messageId } returns msgId
    }

    @Before
    fun before() {
        MockKAnnotations.init(this)
        sbdReceiptFactory = SBDReceiptFactory(serviceRegistryLookup, sbdUtil)

        every { serviceRegistryLookup.getDocumentIdentifier(any(), any()) } returns status
    }

    @Test
    fun `test status creation from arkivmelding message`() {
        every { sbdUtil.isArkivmelding(sbd) } returns true

        val statusSbd = sbdReceiptFactory.createStatusFrom(sbd, DocumentType.STATUS, ReceiptStatus.LEVERT)

        assertEquals(statusSbd.receiverIdentifier, senderOrgnr)
        assertEquals(statusSbd.senderIdentifier, receiverOrgnr)
        assertEquals(Process.ARKIVMELDING_RESPONSE.value, statusSbd.process)
        assertTrue(statusSbd.any is StatusMessage)
        assertEquals(ReceiptStatus.LEVERT, (statusSbd.any as StatusMessage).status)
    }

    @Test
    fun `test status creation from einnsyn message`() {
        every { sbdUtil.isArkivmelding(sbd) } returns false
        every { sbdUtil.isEinnsyn(sbd) } returns true

        val statusSbd = sbdReceiptFactory.createStatusFrom(sbd, DocumentType.STATUS, ReceiptStatus.LEVERT)
        assertEquals(Process.EINNSYN_RESPONSE.value, statusSbd.process)
    }
}