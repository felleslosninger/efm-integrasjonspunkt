package no.difi.meldingsutveksling;

import no.altinn.schemas.services.intermediary.receipt._2009._10.*;
import no.altinn.schemas.services.intermediary.shipment._2009._10.ReferenceType;
import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.intermediary.receipt._2009._10.ReceiptExternalBasicSF;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class AltinnClient {
    public Receipt send(UploadRequest request) {
        // må ha inn properties for ssh key file name og url for sftp server
        SFtpClient sftpClient = new SFtpClient("localhost");
        try (SFtpClient.Connection connection = sftpClient.connect("test_key")) {
            AltinnPackage altinnPackage = AltinnPackage.from(request);
            connection.upload(altinnPackage);
            getReceipt(request); // TODO: add receipt handling
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Receipt();
    }

    public void download(String fileName) {
        SFtpClient sFtpClient = new SFtpClient("localhost");
        try (SFtpClient.Connection connection = sFtpClient.connect("test_key")) {
            InputStream inputStream = connection.getInputStream(fileName);
            AltinnPackage altinnPackage = AltinnPackage.from(inputStream);
            printIt(altinnPackage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printIt(AltinnPackage altinnPackage) {
        try {
            JAXBContext ctx = JAXBContextFactory.createContext(new Class[]{EduDocument.class}, null);
            Marshaller marshaller = ctx.createMarshaller();
            no.difi.meldingsutveksling.domain.sbdh.ObjectFactory objectFactory = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory();
            marshaller.marshal(objectFactory.createStandardBusinessDocument(altinnPackage.getEduDocument()), System.out);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }


    private ReceiptExternal getReceipt(UploadRequest request) {
        try {
            ReceiptExternalBasicSF abinn;
            abinn = new ReceiptExternalBasicSF(new URL("http://localhost:7777/altinn/receipt"), new QName("http://www.altinn.no/services/Intermediary/Receipt/2009/10", "IReceiptExternalBasicImplService"));
            try {

                ReferenceList referenceList = new ReferenceList();
                Reference reference = new Reference();
                reference.setReferenceTypeName(ReferenceType.SENDERS_REFERENCE);
                ObjectFactory objectFactory = new ObjectFactory();
                reference.setReferenceValue(objectFactory.createReferenceReferenceValue(request.getSenderReference()));
                referenceList.getReference().add(reference);
                ReceiptSearchExternal receiptSearchExternal = objectFactory.createReceiptSearchExternal();
                receiptSearchExternal.setReferences(objectFactory.createReceiptExternalReferences(referenceList));
                ReceiptExternal receiptBasic = abinn.getBasicHttpBindingIReceiptExternalBasic().getReceiptBasic("username", "password", receiptSearchExternal);
                System.out.println(receiptBasic);
                printIt(receiptBasic);
                return receiptBasic;
            } catch (IReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage iReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage) {
                iReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage.printStackTrace();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printIt(ReceiptExternal receiptBasic) {
        try {

            JAXBContext ctx = JAXBContextFactory.createContext(new Class[]{ReceiptExternal.class}, null);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(new ObjectFactory().createReceiptExternal(receiptBasic), System.out);
        }
        catch (Exception
                e) {
            e.printStackTrace();
            //catch exception
        }
    }
}
