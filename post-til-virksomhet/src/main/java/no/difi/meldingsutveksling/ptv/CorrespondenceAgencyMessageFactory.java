package no.difi.meldingsutveksling.ptv;

import com.google.common.base.Strings;
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.MyInsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ObjectFactory;
import no.altinn.schemas.services.serviceengine.notification._2009._10.*;
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import org.joda.time.DateTime;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.queryPayload;

/**
 * Class used to create an InsertCorrespondenceV2 object based on a PutMessageRequest(Wrapper).
 *
 * Created by kons-mwa on 01.06.2016.
 */
public class CorrespondenceAgencyMessageFactory {

    private static final Map<Integer, String> serviceEditionMapping= new HashMap<>();

    static {
        serviceEditionMapping.put(1, "Plan, bygg og geodata");
        serviceEditionMapping.put(2, "Helse, sosial og omsorg");
        serviceEditionMapping.put(3, "Oppvekst og utdanning");
        serviceEditionMapping.put(4, "Kultur, idrett og fritid");
        serviceEditionMapping.put(5, "Trafikk, reiser og samferdsel");
        serviceEditionMapping.put(6, "Natur og miljø");
        serviceEditionMapping.put(7, "Næringsutvikling");
        serviceEditionMapping.put(8, "Skatter og avgifter");
        serviceEditionMapping.put(9, "Tekniske tjenester");
        serviceEditionMapping.put(10, "Administrasjon");
    }

    public static InsertCorrespondenceV2 create(CorrespondenceAgencyConfiguration postConfig, PutMessageRequestWrapper msg) throws PayloadException {
        String xpathJpInnhold= "Melding/journpost/jpInnhold";
        String xpathJpOffinnhold= "Melding/journpost/jpOffinnhold";
        String xpathJpFilnavn= "Melding/journpost/dokument/veFilnavn";
        String xpathJpData= "Melding/journpost/dokument/fil/base64";

        ObjectFactory objectFactory = new ObjectFactory();

        String systemUserCode = postConfig.getSystemUserCode(); // "AAS_TEST" TODO: Avklares
        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();

        // Service code, default 4255
        String serviceCodeProp= postConfig.getExternalServiceCode();
        String serviceCode= !Strings.isNullOrEmpty(serviceCodeProp) ? serviceCodeProp : "4255";
        correspondence.setServiceCode(objectFactory.createMyInsertCorrespondenceV2ServiceCode(serviceCode));

        // Service edition, default 10
        String serviceEditionProp = postConfig.getExternalServiceEditionCode();
        String serviceEdition = !Strings.isNullOrEmpty(serviceEditionProp) ? serviceEditionProp : "10";
        correspondence.setServiceEdition(objectFactory.createMyInsertCorrespondenceV2ServiceEdition(serviceEdition));

        // Orgnr. of message recipient.
        correspondence.setReportee(objectFactory.createMyInsertCorrespondenceV2Reportee(msg.getEnvelope().getReceiver().getOrgnr()));

        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode("1044"));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle(queryPayload(msg, xpathJpInnhold)));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary(queryPayload(msg, xpathJpInnhold)));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody(queryPayload(msg, xpathJpOffinnhold)));

        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
        // FunctionType
        binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        // Actual file name of the attachment
        binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName(queryPayload(msg, xpathJpFilnavn)));
        // Name of the attachment
        binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name("Brev"));
        // Has the attachment been encrypted
        binaryAttachmentV2.setEncrypted(false);
        // A unique senders reference
        binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));
        // Attachement data, base64 encoded
        binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(queryPayload(msg, xpathJpData).getBytes()));

        attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachmentExternalBEV2List));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(attachmentsV2));
        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(externalContentV2));

        // The date and time the message should be visible in the Portal
        correspondence.setVisibleDateTime(toXmlGregorianCalendar(DateTime.now()));
        // The date and time the message can be deleted by the user
        correspondence.setAllowSystemDeleteDateTime(
                objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(
                        toXmlGregorianCalendar(DateTime.now().plusMinutes(5))));

        NotificationBEList notifications = new NotificationBEList();
        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        // The date and time the notification should be sent
        notification.setShipmentDateTime(toXmlGregorianCalendar(DateTime.now().plusMinutes(5)));
        // Language code of the notification
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode("1044"));
        // Notification type
        notification.setNotificationType(notificationFactory.createNotification2009NotificationType("offentlig_etat"));

        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();
        // $envelope/sender/orgnr - trenger en navnmapping TODO: Skal hentes fra SR basert paa orgnr
        tokens.getTextToken().add(createTextToken(0, msg.getEnvelope().getSender().getName()));
        // Message area, based on ServiceEdition
        tokens.getTextToken().add(createTextToken(1, serviceEditionMapping.get(Integer.valueOf(serviceEdition))));
        // Name of the message recipient TODO: Skal hentes fra SR basert paa orgnr
        tokens.getTextToken().add(createTextToken(2, msg.getEnvelope().getReceiver().getName()));

        notification.setTextTokens(notificationFactory.createNotification2009TextTokens(tokens));

        JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint();
        notification.setReceiverEndPoints(receiverEndpoints);

        // Should the user be allowed to forward the message from portal
        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(false));
        // Name of the message sender, always "Avsender"
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender("Avsender"));

        notifications.getNotification().add(notification);
        correspondence.setNotifications(objectFactory.createMyInsertCorrespondenceV2Notifications(notifications));

        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory correspondenceObjectFactory = new no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory();
        final InsertCorrespondenceV2 myInsertCorrespondenceV2 = correspondenceObjectFactory.createInsertCorrespondenceV2();
        myInsertCorrespondenceV2.setCorrespondence(correspondence);
        myInsertCorrespondenceV2.setSystemUserCode(systemUserCode);
        // Reference set by the message sender
        myInsertCorrespondenceV2.setExternalShipmentReference(msg.getEnvelope().getConversationId());

        return myInsertCorrespondenceV2;
    }


    private static TextToken createTextToken(int num, String value) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        TextToken textToken = new TextToken();
        textToken.setTokenNum(num);
        textToken.setTokenValue(objectFactory.createTextTokenTokenValue(value));

        return textToken;
    }

    private static JAXBElement<ReceiverEndPointBEList> createReceiverEndPoint() {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        ReceiverEndPoint receiverEndPoint = new ReceiverEndPoint();
        receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(TransportType.fromValue("Email")));
        ReceiverEndPointBEList receiverEndpoints = new ReceiverEndPointBEList();
        receiverEndpoints.getReceiverEndPoint().add(receiverEndPoint);
        return objectFactory.createNotification2009ReceiverEndPoints(receiverEndpoints);
    }

    private static XMLGregorianCalendar toXmlGregorianCalendar(DateTime date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toGregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Could not convert DateTime to " + XMLGregorianCalendar.class, e);
        }
    }


}
