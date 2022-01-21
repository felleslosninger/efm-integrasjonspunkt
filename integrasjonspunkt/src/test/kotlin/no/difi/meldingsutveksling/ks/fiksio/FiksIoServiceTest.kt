package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile
import no.difi.meldingsutveksling.nextmove.FiksIoMessage
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.pipes.PromiseMaker
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.model.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

internal class FiksIoServiceTest {

    @MockK
    lateinit var fiksIOKlient: FiksIOKlient

    @MockK
    lateinit var serviceRegistryLookup: ServiceRegistryLookup

    @MockK
    lateinit var persister: OptionalCryptoMessagePersister

    @MockK
    lateinit var objectMapper: ObjectMapper

    @MockK
    lateinit var promiseMaker: PromiseMaker

    @MockK
    lateinit var conversationService: ConversationService

    private lateinit var fiksIoService: FiksIoService

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this)
        fiksIoService = FiksIoService(fiksIOKlient, serviceRegistryLookup, persister, conversationService, promiseMaker)
    }

    @AfterEach
    fun after() {
        clearStaticMockk(SBDUtil::class)
    }

    @Test
    fun `send message ok`() {
        val orgnr = "910076787"
        val kontoId = "d49177d3-ec0c-40ee-ace9-0f2781a05f45"
        val messageId = "0e238873-63ba-4993-84e1-73b91eb2061d"
        val convId = "c9f37b22-cf8a-44de-b854-050f6a9acc7a"
        val protocol = "digdir.einnsyn.v1"

        val sr = ServiceRecord(ServiceIdentifier.DPFIO, orgnr, "pem123", kontoId)
        sr.process = protocol
        sr.documentTypes = listOf(protocol)
        every { serviceRegistryLookup.getServiceRecord(any(), any()) } returns sr

        every { conversationService.registerStatus(any(), ofType(ReceiptStatus::class)) } returns Optional.empty()

        val sbd = StandardBusinessDocument()
            .setAny(FiksIoMessage()
                .setSikkerhetsnivaa(3))

        mockkStatic(SBDUtil::class)
        every { SBDUtil.getConversationId(sbd) } returns convId
        every { SBDUtil.getMessageId(sbd) } returns messageId
        every { SBDUtil.getProcess(sbd) } returns protocol
//        every { SBDUtil.getReceiverIdentifier(sbd) } returns orgnr
//        every { SBDUtil.getSenderIdentifier(sbd) } returns orgnr
        every { SBDUtil.getDocumentType(sbd) } returns protocol

        val msg = NextMoveOutMessage(
            convId,
            messageId,
            protocol,
            orgnr,
            orgnr,
            ServiceIdentifier.DPFIO,
            sbd
        )
        msg.setFiles(HashSet());

        val payload = StringPayload("foo", "foo.txt")
        val sentMsg = mockkClass(SendtMelding::class)
        every { sentMsg.meldingId } returns MeldingId(UUID.fromString(messageId))

        val requestSlot = slot<MeldingRequest>()
        every { fiksIOKlient.send(capture(requestSlot), any<List<Payload>>()) } returns sentMsg

        fiksIoService.createRequest(msg, listOf(payload))

        verify { fiksIOKlient.send(any(), any<List<Payload>>()) }
        assertEquals(kontoId, requestSlot.captured.mottakerKontoId.toString())
        assertEquals(protocol, requestSlot.captured.meldingType)
    }
}