package no.difi.meldingsutveksling;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schemas.services.intermediary.receipt._2009._10.*;
import no.altinn.schemas.services.intermediary.shipment._2009._10.ReferenceType;
import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.intermediary.receipt._2009._10.ReceiptExternalBasicSF;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.core.io.Resource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
public class AltinnClient {
    @SneakyThrows
    public Receipt send(UploadRequest request) {
        // må ha inn properties for ssh key file name og url for sftp server
        SFtpClient sftpClient = new SFtpClient("localhost");
        try (SFtpClient.Connection connection = sftpClient.connect("test_key")) {
            AltinnPackage altinnPackage = AltinnPackage.from(request);
            connection.upload(altinnPackage);
            getReceipt(request); // TODO: add receipt handling
        }
        return new Receipt();
    }

    public void download(String fileName) {
        SFtpClient sFtpClient = new SFtpClient("localhost");
        try (SFtpClient.Connection connection = sFtpClient.connect("test_key")) {
            Resource altinnZip = connection.getResource(fileName);
            AltinnPackage altinnPackage = AltinnPackage.from(altinnZip);
            printIt(altinnPackage);
        } catch (Exception e) {
            throw new MeldingsUtvekslingRuntimeException("SFTP connection failed", e);
        }
    }

    @SuppressWarnings("squid:S106")
    private void printIt(AltinnPackage altinnPackage) {
        try {
            JAXBContext ctx = JAXBContextFactory.createContext(new Class[]{StandardBusinessDocument.class}, null);
            Marshaller marshaller = ctx.createMarshaller();
            no.difi.meldingsutveksling.domain.sbdh.ObjectFactory objectFactory = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory();
            marshaller.marshal(objectFactory.createStandardBusinessDocument(altinnPackage.getSbd()), System.out);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Couldn't marshall SBD", e);
        }
    }

    private ReceiptExternal getReceipt(UploadRequest request) {
        try {
            ReferenceList referenceList = new ReferenceList();
            Reference reference = new Reference();
            reference.setReferenceTypeName(ReferenceType.SENDERS_REFERENCE);
            ObjectFactory objectFactory = new ObjectFactory();
            reference.setReferenceValue(objectFactory.createReferenceReferenceValue(request.getSenderReference()));
            referenceList.getReference().add(reference);
            ReceiptSearchExternal receiptSearchExternal = objectFactory.createReceiptSearchExternal();
            receiptSearchExternal.setReferences(objectFactory.createReceiptExternalReferences(referenceList));
            ReceiptExternalBasicSF abinn = getReceiptExternalBasicSF();
            ReceiptExternal receiptBasic = abinn.getBasicHttpBindingIReceiptExternalBasic().getReceiptBasic("username", "password", receiptSearchExternal);
            log.info(receiptBasic.toString());
            printIt(receiptBasic);
            return receiptBasic;
        } catch (IReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage e) {
            throw new MeldingsUtvekslingRuntimeException(e.getMessage(), e);
        }
    }

    private ReceiptExternalBasicSF getReceiptExternalBasicSF() {
        try {
            return new ReceiptExternalBasicSF(new URL("http://localhost:7777/altinn/receipt"), new QName("http://www.altinn.no/services/Intermediary/Receipt/2009/10", "IReceiptExternalBasicImplService"));
        } catch (MalformedURLException e) {
            throw new MeldingsUtvekslingRuntimeException("Malformed URL", e);
        }
    }

    private static void printIt(ReceiptExternal receiptBasic) {
        try {
            JAXBContext ctx = JAXBContextFactory.createContext(new Class[]{ReceiptExternal.class}, null);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            OutputStream os = new ByteArrayOutputStream();
            marshaller.marshal(new ObjectFactory().createReceiptExternal(receiptBasic), os);
            String xml = os.toString();
            log.info(xml);
        } catch (Exception e) {
            throw new MeldingsUtvekslingRuntimeException("Couldn't marshall ReceiptExternal", e);
        }
    }
}
