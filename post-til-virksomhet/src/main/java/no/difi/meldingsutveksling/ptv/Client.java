package no.difi.meldingsutveksling.ptv;

import no.difi.meldingsutveksling.altinn.postvirksomhet.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

public class Client {
    public void insertCorrespondence() {
        ObjectFactory objectFactory = new ObjectFactory();

        ICorrespondenceAgencyExternal iCorrespondenceAgencyExternal;
        URL url;
        try {
            url = new URL("http://tt02.altinn.basefarm.net/ServiceEngineExternal/CorrespondenceAgencyExternal.svc?wsdl");
        } catch (MalformedURLException e) {
            throw new RuntimeException("The URL to Altinn Correspondence Agency is malformed", e);
        }
        CorrespondenceAgencyExternalSF correspondenceAgencyExternalSF = new CorrespondenceAgencyExternalSF(url);
        correspondenceAgencyExternalSF.setHandlerResolver(new HeaderHandlerResolver());


        ICorrespondenceAgencyExternal correpondenceService = correspondenceAgencyExternalSF.getCustomBindingICorrespondenceAgencyExternal();
        BindingProvider bp = (BindingProvider) correpondenceService;

        String systemUserCode = "AAS_TEST";
        String externalReference = "12345678";
        MyInsertCorrespondenceV2 correspondence = new MyInsertCorrespondenceV2();
        correspondence.setServiceCode(objectFactory.createMyInsertCorrespondenceV2ServiceCode("4255"));
        correspondence.setServiceEdition(objectFactory.createMyInsertCorrespondenceV2ServiceEdition("4"));
        correspondence.setReportee(objectFactory.createStatusV2Reportee("910926551"));
        ExternalContentV2 externalContentV2 = new ExternalContentV2();
        externalContentV2.setLanguageCode(objectFactory.createString("1044"));
        externalContentV2.setMessageTitle(objectFactory.createString("Dette er en test"));
        externalContentV2.setMessageSummary(objectFactory.createString("Her kommmer meldingssammendraget."));
        externalContentV2.setMessageBody(objectFactory.createString("&lt;html>Dette er en test&lt;/html>"));
        AttachmentsV2 attachmentsV2 = new AttachmentsV2();
        BinaryAttachmentExternalBEV2List attachmentExternalBEV2List = new BinaryAttachmentExternalBEV2List();
        ;
        BinaryAttachmentV2 binaryAttachmentV2 = new BinaryAttachmentV2();
        binaryAttachmentV2.setFunctionType(AttachmentFunctionType.fromValue("Unspecified"));
        binaryAttachmentV2.setFileName(objectFactory.createString("brev_med_innhold.pdf"));
        binaryAttachmentV2.setEncrypted(false);
        binaryAttachmentV2.setSendersReference(objectFactory.createString("asdfasdf"));
//        binaryAttachmentV2.setData(objectFactory.createBase64Binary(""));

        attachmentExternalBEV2List.getBinaryAttachmentV2().add(binaryAttachmentV2);
        attachmentsV2.setBinaryAttachments(objectFactory.createBinaryAttachmentExternalBEV2List(attachmentExternalBEV2List));
        externalContentV2.setAttachments(objectFactory.createAttachmentsV2(attachmentsV2));
        correspondence.setContent(objectFactory.createExternalContentV2(externalContentV2));

        //TODO
//        correspondence.setVisibleDateTime(objectFactory.createDateTime(new Date()));
        try {
            correpondenceService.insertCorrespondenceV2(systemUserCode, externalReference, correspondence);
        } catch (ICorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage iCorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage) {
            iCorrespondenceAgencyExternalInsertCorrespondenceV2AltinnFaultFaultFaultMessage.printStackTrace();
        }
    }
}
