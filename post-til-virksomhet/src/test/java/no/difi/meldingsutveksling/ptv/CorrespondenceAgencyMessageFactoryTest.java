package no.difi.meldingsutveksling.ptv;

import lombok.SneakyThrows;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.MyInsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.notification._2009._10.Notification2009;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrespondenceAgencyMessageFactoryTest {

    @Mock(lenient = true)
    CorrespondenceAgencyConfiguration config;
    @Mock
    IntegrasjonspunktProperties props;
    @Mock
    ServiceRegistryLookup serviceRegistryLookup;
    @Mock
    OptionalCryptoMessagePersister cryptoMessagePersister;
    @Spy
    Clock clock = Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DateTimeUtil.DEFAULT_ZONE_ID);
    @Spy
    ReporteeFactory reporteeFactory = new ReporteeFactory();
    @Spy
    ArkivmeldingUtil arkivmeldingUtil = new ArkivmeldingUtil();

    @InjectMocks
    CorrespondenceAgencyMessageFactory messageFactory;

    @Mock
    NextMoveOutMessage msg;

    StandardBusinessDocument sbd;

    private static final String SENDER_ORGNR = "123456789";
    private static final String SENDER_ORGNAME = "ACME Corp.";
    private static final String RECEIVER_ORGNR = "987654321";
    private static final String NOTIFICATION_TEXT = "Melding til $reporteeName$ fra $reporterName$";
    private static final String NOTIFICATION_TEXT_SENSITIVE = "Taushetsbelagt melding til $reporteeName$ fra $reporterName$";
    private static final String PROCESS = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static final String PROCESS_SENSITIVE = "urn:no:difi:profile:arkivmelding:taushetsbelagtAdministrasjon:ver1.0";
    private static final String SERVICE_CODE = "4255";
    private static final String SERVICE_CODE_SENSITIVE = "5504";

    @BeforeEach
    void beforeEach() {
        when(config.getSensitiveServiceCode()).thenReturn(SERVICE_CODE_SENSITIVE);
        when(config.getNotificationText()).thenReturn(NOTIFICATION_TEXT);
        when(config.getSensitiveNotificationText()).thenReturn(NOTIFICATION_TEXT_SENSITIVE);
        when(config.isNotifyEmail()).thenReturn(true);
        when(config.isNotifySms()).thenReturn(true);

        IntegrasjonspunktProperties.Organization org = mock(IntegrasjonspunktProperties.Organization.class);
        when(org.getNumber()).thenReturn(SENDER_ORGNR);
        when(props.getOrg()).thenReturn(org);
        IntegrasjonspunktProperties.PostVirksomheter dpv = mock(IntegrasjonspunktProperties.PostVirksomheter.class);
        lenient().when(dpv.getDaysToReply()).thenReturn(7L);
        lenient().when(props.getDpv()).thenReturn(dpv);

        InfoRecord infoRecord = mock(InfoRecord.class);
        when(infoRecord.getOrganizationName()).thenReturn(SENDER_ORGNAME);
        when(serviceRegistryLookup.getInfoRecord(SENDER_ORGNR)).thenReturn(infoRecord);

        when(msg.getReceiverIdentifier()).thenReturn(RECEIVER_ORGNR);

        this.sbd = spy(new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .addSender(new Partner())
                        .addReceiver(new Partner())
                        .setDocumentIdentification(new DocumentIdentification()
                                .setStandard("standard")
                        )
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .setType(ScopeType.CONVERSATION_ID.getFullname())
                                )
                        )
                ));

        when(msg.getSbd()).thenReturn(sbd);
    }

    @SneakyThrows
    void setupForProcessAndServiceCode(String process, String serviceCode) {
        ServiceRecord serviceRecord = mock(ServiceRecord.class);
        Service serviceRecordService = new Service();
        serviceRecordService.setServiceCode(serviceCode);
        serviceRecordService.setIdentifier(ServiceIdentifier.DPV);
        when(serviceRecord.getService()).thenReturn(serviceRecordService);
        sbd.getScope(ScopeType.CONVERSATION_ID)
                .ifPresent(scope -> scope.setIdentifier(process));
        when(serviceRegistryLookup.getServiceRecord(any(), any())).thenReturn(serviceRecord);
    }

    @Test
    void testCreateDefaultNotifications() {
        setupForProcessAndServiceCode(PROCESS, SERVICE_CODE);

        List<Notification2009> notifications = ReflectionTestUtils.invokeMethod(messageFactory, "createNotifications", msg);
        assertNotNull(notifications);
        Notification2009 notification = notifications.get(0);
        assertEquals(TransportType.BOTH, notification.getReceiverEndPoints().getValue().getReceiverEndPoint().get(0).getTransportType().getValue());
        assertEquals(DpvVarselType.VARSEL_DPV_MED_REVARSEL.getFullname(), notification.getNotificationType().getValue());
        assertEquals(NOTIFICATION_TEXT.replace("$reporterName$", SENDER_ORGNAME), notification.getTextTokens().getValue().getTextToken().get(0).getTokenValue().getValue());
    }

    @Test
    void testOverrideVarselTypeAndTransportType() {
        ArkivmeldingMessage amMsg = new ArkivmeldingMessage();
        DpvSettings dpv = new DpvSettings();
        dpv.setVarselType(DpvVarselType.VARSEL_DPV_UTEN_REVARSEL);
        dpv.setVarselTransportType(DpvVarselTransportType.EPOST);
        String varselTekstOverride = "foo $reporterName$ bar $reporteeName$";
        dpv.setVarselTekst(varselTekstOverride);
        amMsg.setDpv(dpv);
        when(msg.getBusinessMessage()).thenReturn((BusinessMessage) amMsg);

        setupForProcessAndServiceCode(PROCESS, SERVICE_CODE);
        List<Notification2009> notifications = ReflectionTestUtils.invokeMethod(messageFactory, "createNotifications", msg);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        Notification2009 notification = notifications.get(0);
        assertEquals(DpvVarselType.VARSEL_DPV_UTEN_REVARSEL.getFullname(), notification.getNotificationType().getValue());
        assertEquals(TransportType.EMAIL, notification.getReceiverEndPoints().getValue().getReceiverEndPoint().get(0).getTransportType().getValue());
        assertEquals(varselTekstOverride.replace("$reporterName$", SENDER_ORGNAME), notification.getTextTokens().getValue().getTextToken().get(0).getTokenValue().getValue());
    }

    @Test
    void testDefaultSensitivVarsel() {
        setupForProcessAndServiceCode(PROCESS_SENSITIVE, SERVICE_CODE_SENSITIVE);

        List<Notification2009> notifications = ReflectionTestUtils.invokeMethod(messageFactory, "createNotifications", msg);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        Notification2009 notification = notifications.get(0);
        assertEquals(NOTIFICATION_TEXT_SENSITIVE.replace("$reporterName$", SENDER_ORGNAME), notification.getTextTokens().getValue().getTextToken().get(0).getTokenValue().getValue());
    }

    @Test
    void testSensitivVarselOverride() {
        ArkivmeldingMessage amMsg = new ArkivmeldingMessage();
        DpvSettings dpv = new DpvSettings();
        String varselTekst = "taus foo $reporterName$ bar $reporteeName$";
        dpv.setTaushetsbelagtVarselTekst(varselTekst);
        amMsg.setDpv(dpv);
        when(msg.getBusinessMessage()).thenReturn((BusinessMessage) amMsg);

        setupForProcessAndServiceCode(PROCESS_SENSITIVE, SERVICE_CODE_SENSITIVE);

        List<Notification2009> notifications = ReflectionTestUtils.invokeMethod(messageFactory, "createNotifications", msg);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        Notification2009 notification = notifications.get(0);
        assertEquals(varselTekst.replace("$reporterName$", SENDER_ORGNAME), notification.getTextTokens().getValue().getTextToken().get(0).getTokenValue().getValue());
    }

    @Test
    void testOverrideSvarfrist() {
        ArkivmeldingMessage amMsg = new ArkivmeldingMessage();
        DpvSettings dpv = new DpvSettings();
        dpv.setDagerTilSvarfrist(3);
        amMsg.setDpv(dpv);
        when(msg.getBusinessMessage()).thenReturn((BusinessMessage) amMsg);

        setupForProcessAndServiceCode(PROCESS, SERVICE_CODE);
        MyInsertCorrespondenceV2 correspondence = ReflectionTestUtils.invokeMethod(messageFactory, "getMyInsertCorrespondenceV2", msg, "title", "summary", "body", null);
        assertNotNull(correspondence);
        assertEquals(28, correspondence.getDueDateTime().getDay());
    }

}