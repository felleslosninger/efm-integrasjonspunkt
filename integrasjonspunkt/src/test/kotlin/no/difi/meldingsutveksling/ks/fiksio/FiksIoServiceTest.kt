package no.difi.meldingsutveksling.ks.fiksio

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage
import no.difi.meldingsutveksling.pipes.Plumber
import no.difi.meldingsutveksling.pipes.PromiseMaker
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord
import no.ks.fiks.io.client.FiksIOKlient
import no.ks.fiks.io.client.model.*
import org.junit.Before
import org.junit.Test
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
    lateinit var plumber: Plumber

    @MockK
    lateinit var promiseMaker: PromiseMaker

    private lateinit var fiksIoService: FiksIoService

    @Before
    fun before() {
        MockKAnnotations.init(this)
        fiksIoService = FiksIoService(fiksIOKlient, serviceRegistryLookup, persister, objectMapper, plumber, promiseMaker)
    }

    @Test
    fun `send message ok`() {
        val orgnr = "910076787"
        val kontoId = "d49177d3-ec0c-40ee-ace9-0f2781a05f45"
        val sr = ServiceRecord(ServiceIdentifier.DPFIO, orgnr, "pem123", kontoId)
        sr.service.serviceCode = "no.digdir.einnsyn.v1"
        every { serviceRegistryLookup.getReceiverServiceRecord(any(), any<String>()) } returns sr

        val messageId = "0e238873-63ba-4993-84e1-73b91eb2061d"
        val conversationId = "c9f37b22-cf8a-44de-b854-050f6a9acc7a"
        val process = "urn:no:difi:profile:einnsyn:innsynskrav:ver1.0"
        val documenttype = "urn:no:difi:einnsyn:xsd::innsynskrav"
        val sbd = mockkClass(StandardBusinessDocument::class)
        every { sbd.conversationId } returns conversationId
        every { sbd.documentId } returns messageId
        every { sbd.process } returns process
        every { sbd.receiverIdentifier } returns orgnr
        every { sbd.senderIdentifier } returns orgnr
        every { sbd.documentType } returns documenttype
        val msg = NextMoveOutMessage.of(sbd, ServiceIdentifier.DPFIO)

        val payload = StringPayload("foo", "foo.txt")
        val sentMsg = mockkClass(SendtMelding::class)
        every { sentMsg.meldingId } returns MeldingId(UUID.fromString(messageId))

        val requestSlot = slot<MeldingRequest>()
        every { fiksIOKlient.send(capture(requestSlot), any<List<Payload>>()) } returns sentMsg

        fiksIoService.createRequest(msg, listOf(payload))

        verify { fiksIOKlient.send(any(), any<List<Payload>>()) }
        assertEquals(kontoId, requestSlot.captured.mottakerKontoId.toString())
        assertEquals(sr.service.serviceCode, requestSlot.captured.meldingType)
    }
}