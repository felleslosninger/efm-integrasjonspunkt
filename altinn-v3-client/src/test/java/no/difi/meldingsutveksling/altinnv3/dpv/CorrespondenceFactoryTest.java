package no.difi.meldingsutveksling.altinnv3.dpv;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.PostVirksomheter;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.DpvSettings;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Service;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondenceNotificationExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CorrespondenceFactoryTest {

    private CorrespondenceFactory correspondenceFactory;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private IntegrasjonspunktProperties properties;

    @Mock
    private DpvHelper dpvHelper;

    @Mock
    private ServiceRegistryHelper serviceRegistryHelper;

    private final Clock clock = Clock.fixed(OffsetDateTime.now().toInstant(), OffsetDateTime.now().getOffset());

    private NextMoveOutMessage message;
    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "123454321");
    private static final String MESSAGE_TITLE = "";
    private static final String MESSAGE_BODY = "";
    private static final String MESSAGE_SUMMARY = "";
    private static final String ALTINN_RESOURCE_ID = "Altinn resource id";


    @BeforeEach
    public void setup() {

        correspondenceFactory = new CorrespondenceFactory(
            notificationFactory,
            clock,
            dpvHelper,
            serviceRegistryHelper,
            properties
        );

        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setService(new Service().setResource(ALTINN_RESOURCE_ID));


        when(serviceRegistryHelper.getServiceRecord(Mockito.any())).thenReturn(serviceRecord);
        when(properties.getDpv()).thenReturn(new PostVirksomheter()
            .setSensitiveResource("sensitive resource id"));

        StandardBusinessDocument standardBusinessDocument = new StandardBusinessDocument();
        standardBusinessDocument.setSenderIdentifier(SENDER);
        standardBusinessDocument.setStandardBusinessDocumentHeader(
            standardBusinessDocument.getStandardBusinessDocumentHeader()
                .setDocumentIdentification(
                    new DocumentIdentification().setStandard("DummyValue")
                )
        );

        message = new NextMoveOutMessage();
        message.setReceiverIdentifier("222222222");
        message.setSbd(standardBusinessDocument);
    }

    @Test
    public void create_mapsAttachments() {
        BusinessMessageFile businessMessageFile = new BusinessMessageFile()
            .setFilename("Filename 1")
            .setTitle("Title 1");
        BusinessMessageFile businessMessageFile2 = new BusinessMessageFile()
            .setFilename("Filename 2")
            .setTitle("Title 2");

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, List.of(businessMessageFile, businessMessageFile2));

        assertEquals(2, result.getCorrespondence().getContent().getAttachments().size());

        assertEquals("Filename 1", result.getCorrespondence().getContent().getAttachments().getFirst().getFileName());
        assertEquals("Title 1", result.getCorrespondence().getContent().getAttachments().getFirst().getDisplayName());
        assertEquals(false, result.getCorrespondence().getContent().getAttachments().getFirst().getIsEncrypted());
        assertEquals("AttachmentReference_as123452", result.getCorrespondence().getContent().getAttachments().getFirst().getSendersReference());

        assertEquals("Filename 2", result.getCorrespondence().getContent().getAttachments().getLast().getFileName());
        assertEquals("Title 2", result.getCorrespondence().getContent().getAttachments().getLast().getDisplayName());
        assertEquals(false, result.getCorrespondence().getContent().getAttachments().getLast().getIsEncrypted());
        assertEquals("AttachmentReference_as123452", result.getCorrespondence().getContent().getAttachments().getLast().getSendersReference());
    }

    @Test
    public void create_mapsSimpleValuesInContent(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals("nb", result.getCorrespondence().getContent().getLanguage());
        assertEquals(MESSAGE_TITLE, result.getCorrespondence().getContent().getMessageTitle());
        assertEquals(MESSAGE_BODY, result.getCorrespondence().getContent().getMessageBody());
        assertEquals(MESSAGE_SUMMARY, result.getCorrespondence().getContent().getMessageSummary());
    }

    @Test
    public void create_mapsRecipients(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals("urn:altinn:organization:identifier-no:222222222", result.getRecipients().getFirst(), "Should have prefix urn:altinn:organization:identifier-no: if receiver identifier is not 11 digits");
    }

    @Test
    public void create_mapsPrivateRecipients(){
        message.setReceiverIdentifier("12345678911");
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals("urn:altinn:person:identifier-no:12345678911", result.getRecipients().getFirst(), "Should have prefix urn:altinn:person:identifier-no: if receiver identifier is 11 digits");
    }

    @Test
    public void mapExisting(){
        List<UUID> existingList = List.of(UUID.randomUUID(), UUID.randomUUID());
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, existingList, null);

        assertEquals(existingList, result.getExistingAttachments());
    }

    @Test
    public void create_usesNotificationFactory(){
        InitializeCorrespondenceNotificationExt notification = new InitializeCorrespondenceNotificationExt();
        when(notificationFactory.getNotification(message)).thenReturn(notification);

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        verify(notificationFactory).getNotification(message);
        assertEquals(notification, result.getCorrespondence().getNotification());
    }

    @Test
    public void create_mapsResourceId(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(ALTINN_RESOURCE_ID, result.getCorrespondence().getResourceId());
    }

    @Test
    public void create_mapsRequestedPublishTime(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(OffsetDateTime.now(clock), result.getCorrespondence().getRequestedPublishTime());
    }

    @Test
    public void create_mapsMessageSender(){
        String senderName = "Sender name";
        when(serviceRegistryHelper.getSenderName(message)).thenReturn(senderName);

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(senderName, result.getCorrespondence().getMessageSender());
    }

    @Test
    public void create_mapsDefaultDueDateTime(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertNull(result.getCorrespondence().getDueDateTime());
    }

    @Test
    public void create_mapsDueDateTimeFromDpvSettings(){
        when(dpvHelper.getDpvSettings(message)).thenReturn(Optional.ofNullable(new DpvSettings().setDagerTilSvarfrist(15)));

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(OffsetDateTime.now(clock).plusDays(15), result.getCorrespondence().getDueDateTime());
    }

    @Test
    public void create_mapsDefaultDueDateTimeWhenEnableDueDateIsTrue(){
        when(properties.getDpv()).thenReturn(
            new PostVirksomheter()
                .setEnableDueDate(true));

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(OffsetDateTime.now(clock).plusDays(7), result.getCorrespondence().getDueDateTime());
    }

    @Test
    public void create_mapsDefaultDueDateTimeWhenEnableDueDateIsTrueAndDaysToReplyIsSet(){
        when(properties.getDpv()).thenReturn(
            new PostVirksomheter()
                .setEnableDueDate(true)
                .setDaysToReply(22L));

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(OffsetDateTime.now(clock).plusDays(22), result.getCorrespondence().getDueDateTime());
    }

    @Test
    public void create_mapsSender(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(SENDER.getIdentifier(), result.getCorrespondence().getSender());
    }

    @Test
    public void create_mapsPropertyList(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        var expectedResult = new HashMap<String, String>();
        expectedResult.put("senderOrgNumber", SENDER.getOrganizationIdentifier());

        assertEquals(expectedResult, result.getCorrespondence().getPropertyList(), "Should map senderOrgNumbers in propertylist, value is used by Altinn to know who the sender of the message is");
    }

    @Test
    public void create_mapsPropertyList_OnBehalfOf(){

        Iso6523 BehalfOfSender = Iso6523.of(ICD.NO_ORG, "123454321", "88889999");

        message.getSbd().setSenderIdentifier(BehalfOfSender);


        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        var expectedResult = new HashMap<String, String>();
        expectedResult.put("senderOrgNumber", BehalfOfSender.getOrganizationPartIdentifier());

        assertEquals(expectedResult, result.getCorrespondence().getPropertyList(), "Should map senderOrgNumbers in propertylist to behalf of organization number, value is used by Altinn to know who the sender of the message is");
    }

    @Test
    public void create_mapsIsConfirmationNeededToFalse(){
        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(false, result.getCorrespondence().getIsConfirmationNeeded());
    }

    @Test
    public void create_mapsSenderSReference(){
        message.setMessageId("Something");

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(message.getMessageId(), result.getCorrespondence().getSendersReference());
    }

    @ParameterizedTest(name = "When resource is confidential = {0}, then map isConfidential = {0} on correspondence")
    @ValueSource(booleans = {true, false})
    public void create_mapsIsConfidentialBasedUponResource(boolean confidential) {
        when(dpvHelper.isConfidential(Mockito.any())).thenReturn(confidential);

        var result = correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);

        assertEquals(confidential, result.getCorrespondence().getIsConfidential(), "If resource is confidential, it should be mapped correctly on correspondence");
    }

    @Test
    public void throw_error_when_resource_is_null(){
        ServiceRecord serviceRecord = new ServiceRecord();
        serviceRecord.setService(new Service().setResource(null));

        when(serviceRegistryHelper.getServiceRecord(Mockito.any())).thenReturn(serviceRecord);

        Exception exception = assertThrows(NextMoveRuntimeException.class, () -> {
            correspondenceFactory.create(message, MESSAGE_TITLE, MESSAGE_SUMMARY, MESSAGE_BODY, null, null);
        });

        assertEquals("Service Registry returned empty Altinn resource id. Resource id cannot be null or blank, contact Digdir for support.", exception.getMessage());
    }
}
