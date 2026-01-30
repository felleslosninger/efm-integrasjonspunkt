package no.difi.meldingsutveksling.sbd;

import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.StatusMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SbdFactoryTest {

    @Mock
    private ServiceRegistryLookup serviceRegistryLookup;

    @Mock
    private IntegrasjonspunktProperties props;

    @Mock
    private IntegrasjonspunktProperties.Arkivmelding arkivmeldingProps;

    @Mock
    private IntegrasjonspunktProperties.Einnsyn einnsynProps;

    @Mock
    private IntegrasjonspunktProperties.NextMove nextMoveProps;

    @Mock
    private UUIDGenerator uuidGenerator;

    private final Clock clock = Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DateTimeUtil.DEFAULT_ZONE_ID);
    private SBDFactory sbdFactory;

    private final String arkivmeldingProcess = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private final String arkivmeldingResponseProcess = "urn:no:difi:profile:arkivmelding:response:ver1.0";
    private final String einnsynResponseProcess = "urn:no:difi:profile:einnsyn:response:ver1.0";
    private final String statusDocType = "urn:no:difi:eformidling:xsd::status";
    private final PartnerIdentifier sender = PartnerIdentifier.parse("0192:910076787");
    private final PartnerIdentifier receiver = PartnerIdentifier.parse("0192:991825827");
    private final String convId = "e3016cb7-39de-4166-a935-3a574cd2a2db";
    private final String msgId = "4653f436-8921-4224-b824-068f2cc6232f";

    @Mock
    private StandardBusinessDocument sbd;

    private MockedStatic<SBDUtil> sbdUtilMock;

    @BeforeEach
    void before() {
        sbdFactory = new SBDFactory(serviceRegistryLookup, clock, props, uuidGenerator);
        sbdUtilMock = mockStatic(SBDUtil.class);
        sbdUtilMock.when(() -> SBDUtil.getOptionalMessageChannel(sbd)).thenReturn(Optional.empty());
    }

    @AfterEach
    void after() {
        if (sbdUtilMock != null) {
            sbdUtilMock.close();
        }
    }

    @Test
    void test_status_creation_from_arkivmelding_message() {
        sbdUtilMock.when(() -> SBDUtil.isArkivmelding(sbd)).thenReturn(true);
        when(props.getArkivmelding()).thenReturn(arkivmeldingProps);
        when(arkivmeldingProps.getReceiptProcess()).thenReturn(arkivmeldingResponseProcess);
        when(props.getNextmove()).thenReturn(nextMoveProps);
        when(nextMoveProps.getStatusDocumentType()).thenReturn(statusDocType);
        when(nextMoveProps.getDefaultTtlHours()).thenReturn(24);
        when(sbd.getReceiverIdentifier()).thenReturn(receiver);
        when(sbd.getSenderIdentifier()).thenReturn(sender);
        when(sbd.getConversationId()).thenReturn(convId);
        when(sbd.getMessageId()).thenReturn(msgId);


        var statusSbd = sbdFactory.createStatusFrom(sbd, ReceiptStatus.LEVERT);

        assertEquals(sender.getIdentifier(), statusSbd.getReceiverIdentifier().getIdentifier());
        assertEquals(receiver.getIdentifier(), statusSbd.getSenderIdentifier().getIdentifier());
        assertEquals(arkivmeldingResponseProcess, statusSbd.getProcess());
        assertTrue(statusSbd.getAny() instanceof StatusMessage);
        assertEquals(ReceiptStatus.LEVERT, ((StatusMessage) statusSbd.getAny()).getStatus());
    }

    @Test
    void test_status_creation_from_einnsyn_message() {
        sbdUtilMock.when(() -> SBDUtil.isArkivmelding(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isEinnsyn(sbd)).thenReturn(true);
        when(props.getEinnsyn()).thenReturn(einnsynProps);
        when(nextMoveProps.getStatusDocumentType()).thenReturn(statusDocType);
        when(einnsynProps.getReceiptProcess()).thenReturn(einnsynResponseProcess);

        when(props.getNextmove()).thenReturn(nextMoveProps);
        when(nextMoveProps.getStatusDocumentType()).thenReturn(statusDocType);
        when(nextMoveProps.getDefaultTtlHours()).thenReturn(24);

        when(sbd.getReceiverIdentifier()).thenReturn(receiver);
        when(sbd.getSenderIdentifier()).thenReturn(sender);
        when(sbd.getConversationId()).thenReturn(convId);
        when(sbd.getMessageId()).thenReturn(msgId);


        var statusSbd = sbdFactory.createStatusFrom(sbd, ReceiptStatus.LEVERT);
        assertEquals(einnsynResponseProcess, statusSbd.getProcess());
    }

    @Test
    void test_message_type_validation() {
        var serviceRecord = mock(no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.class);
        when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPO);
        try {
            doReturn(serviceRecord).when(serviceRegistryLookup).getServiceRecord(any(), any());
        } catch (no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException e) {
            fail(e);
        }

        assertThrows(MeldingsUtvekslingRuntimeException.class, () ->
                sbdFactory.createNextMoveSBD(
                        sender,
                        receiver,
                        convId, msgId,
                        arkivmeldingProcess,
                        "foo::bar",
                        mock(ArkivmeldingMessage.class)
                )
        );
    }

    @Test
    void unknown_document_type_allowed_for_fiksio_message_type() {
        var serviceRecord = mock(no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.class);
        when(serviceRecord.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPFIO);
        when(props.getNextmove()).thenReturn(nextMoveProps);
        when(nextMoveProps.getDefaultTtlHours()).thenReturn(24);

        try {
            doReturn(serviceRecord).when(serviceRegistryLookup).getServiceRecord(any(), any());
        } catch (no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException e) {
            fail(e);
        }

        sbdFactory.createNextMoveSBD(
                sender,
                receiver,
                convId, msgId,
                arkivmeldingProcess,
                "foo::bar",
                mock(ArkivmeldingMessage.class)
        );
    }
}
