package no.difi.meldingsutveksling.ptv;

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.AttachmentsV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.MyInsertCorrespondenceV2;
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ObjectFactory;
import no.altinn.schemas.services.serviceengine.notification._2009._10.*;
import no.altinn.schemas.services.serviceengine.subscription._2009._10.AttachmentFunctionType;
import no.altinn.services.serviceengine.correspondence._2009._10.CorrespondenceAgencyExternalSF;
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternal;
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentExternalBEV2List;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Client {
    public void createCorrespondence() {
        no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory objectFactory = new no.altinn.services.serviceengine.correspondence._2009._10.ObjectFactory();

        InsertCorrespondenceV2 insertCorrespondenceV2 = objectFactory.createInsertCorrespondenceV2();
        insertCorrespondenceV2.setExternalShipmentReference("SendersReference_AZ12347");
        insertCorrespondenceV2.setSystemUserCode("AAS_TEST");
       // insertCorrespondenceV2.setCorrespondence();

    }

    public void insertCorrespondence() {
        ObjectFactory objectFactory = new ObjectFactory();

        ICorrespondenceAgencyExternal iCorrespondenceAgencyExternal;
        URL url;
        try {
            url = new URL("https://tt02.altinn.basefarm.net/ServiceEngineExternal/CorrespondenceAgencyExternal.svc?wsdl");
        } catch (MalformedURLException e) {
            throw new RuntimeException("The URL to Altinn Correspondence Agency is malformed", e);
        }
        CorrespondenceAgencyExternalSF correspondenceAgencyExternalSF = new CorrespondenceAgencyExternalSF(url);
        correspondenceAgencyExternalSF.setHandlerResolver(new HeaderHandlerResolver());


        ICorrespondenceAgencyExternal correpondenceService = correspondenceAgencyExternalSF.getCustomBindingICorrespondenceAgencyExternal();
        BindingProvider bp = (BindingProvider) correpondenceService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "https://tt02.altinn.basefarm.net/ServiceEngineExternal/CorrespondenceAgencyExternal.svc?wsdl");

        String systemUserCode = "AAS_TEST";
        String externalReference = "12345678";
        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();
        correspondence.setServiceCode(objectFactory.createMyInsertCorrespondenceV2ServiceCode("4255"));
        correspondence.setServiceEdition(objectFactory.createMyInsertCorrespondenceV2ServiceEdition("4"));
        correspondence.setReportee(objectFactory.createMyInsertCorrespondenceV2Reportee("910926551"));

        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createExternalContentV2LanguageCode("1044"));
        externalContentV2.setMessageTitle(objectFactory.createExternalContentV2MessageTitle("Dette er en test"));
        externalContentV2.setMessageSummary(objectFactory.createExternalContentV2MessageSummary("Her kommmer meldingssammendraget."));
        externalContentV2.setMessageBody(objectFactory.createExternalContentV2MessageBody("&lt;html>Dette er en test&lt;/html>"));

        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();

        BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
        binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
        no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory reporteeFactory = new no.altinn.services.serviceengine.reporteeelementlist._2010._10.ObjectFactory();
        binaryAttachmentV2.setFileName(reporteeFactory.createBinaryAttachmentV2FileName("brev_med_innhold.pdf"));
        binaryAttachmentV2.setName(reporteeFactory.createBinaryAttachmentV2Name("Brev"));
        binaryAttachmentV2.setEncrypted(false);
        binaryAttachmentV2.setSendersReference(reporteeFactory.createBinaryAttachmentV2SendersReference("AttachmentReference_as123452"));

        try {
            byte[] data = FileUtils.readFileToByteArray(new File("src/test/resources/data"));
            binaryAttachmentV2.setData(reporteeFactory.createBinaryAttachmentV2Data(data));
        } catch (IOException e) {
            e.printStackTrace();
        }

        attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        attachmentsV2.setBinaryAttachments(objectFactory.createAttachmentsV2BinaryAttachments(attachmentExternalBEV2List));
        externalContentV2.setAttachments(objectFactory.createExternalContentV2Attachments(attachmentsV2));
        correspondence.setContent(objectFactory.createMyInsertCorrespondenceV2Content(externalContentV2));

        XMLGregorianCalendar date = fiveMinutesFromNow();
        correspondence.setVisibleDateTime(date);
        correspondence.setAllowSystemDeleteDateTime(objectFactory.createMyInsertCorrespondenceV2AllowSystemDeleteDateTime(date));

        NotificationBEList notifications = new NotificationBEList();
        Notification2009 notification = new Notification2009();
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory notificationFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        notification.setFromAddress(notificationFactory.createNotification2009FromAddress("no-reply@altinn.no"));
        notification.setShipmentDateTime(date);
        notification.setLanguageCode(notificationFactory.createNotification2009LanguageCode("1044"));
        notification.setNotificationType(notificationFactory.createNotification2009NotificationType("offentlig_etat"));

        TextTokenSubstitutionBEList tokens = new TextTokenSubstitutionBEList();
        tokens.getTextToken().add(createTextToken(0, "Avsender"));
        tokens.getTextToken().add(createTextToken(1, "Idrett og kultur"));
        tokens.getTextToken().add(createTextToken(2, "$reporteeName$"));

        notification.setTextTokens(notificationFactory.createNotification2009TextTokens(tokens));

        JAXBElement<ReceiverEndPointBEList> receiverEndpoints = createReceiverEndPoint();

        notification.setReceiverEndPoints(receiverEndpoints);

        correspondence.setAllowForwarding(objectFactory.createMyInsertCorrespondenceV2AllowForwarding(false));
        correspondence.setMessageSender(objectFactory.createMyInsertCorrespondenceV2MessageSender("Avsender"));

        notifications.getNotification().add(notification);
        correspondence.setNotifications(objectFactory.createMyInsertCorrespondenceV2Notifications(notifications));

        try {
            correpondenceService.insertCorrespondenceV2(systemUserCode, externalReference, correspondence);
        } catch (ICorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage iCorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage) {
            iCorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage.printStackTrace();
        }
    }

    private JAXBElement<ReceiverEndPointBEList> createReceiverEndPoint() {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        ReceiverEndPoint receiverEndPoint = new ReceiverEndPoint();
        receiverEndPoint.setTransportType(objectFactory.createReceiverEndPointTransportType(TransportType.fromValue("Email")));
        ReceiverEndPointBEList receiverEndpoints = new ReceiverEndPointBEList();
        receiverEndpoints.getReceiverEndPoint().add(receiverEndPoint);
        return objectFactory.createNotification2009ReceiverEndPoints(receiverEndpoints);
    }

    private TextToken createTextToken(int num, String value) {
        no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory objectFactory = new no.altinn.schemas.services.serviceengine.notification._2009._10.ObjectFactory();
        TextToken textToken = new TextToken();
        textToken.setTokenNum(num);
        textToken.setTokenValue(objectFactory.createTextTokenTokenValue(value));

        return textToken;
    }

    private XMLGregorianCalendar toXmlGregorianCalendar(DateTime date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toGregorianCalendar());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Could not convert DateTime to " + XMLGregorianCalendar.class, e);
        }
    }

    private XMLGregorianCalendar fiveMinutesFromNow() {
        return toXmlGregorianCalendar(new DateTime().plusMinutes(5));
    }
}
