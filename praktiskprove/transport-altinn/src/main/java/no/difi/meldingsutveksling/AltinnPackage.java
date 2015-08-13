package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ArrayOfRecipient;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Manifest;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Recipient;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.RecipientBuilder;
import no.difi.meldingsutveksling.shipping.Request;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AltinnPackage {
    private final Manifest manifest;
    private final Recipient recipient;
    private final StandardBusinessDocument document;
    private final JAXBContext ctx;

    private AltinnPackage(Manifest manifest, Recipient recipient, StandardBusinessDocument document) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.document = document;
        try {
            ctx = JAXBContext.newInstance(Manifest.class, Recipient.class, StandardBusinessDocument.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static AltinnPackage from(Request document) {
        ManifestBuilder manifest = new ManifestBuilder();
        manifest.withSender(document.getSender());
        manifest.withSenderReference(document.getSenderReference());


        RecipientBuilder recipient = new RecipientBuilder(document.getReceiver());
        return new AltinnPackage(manifest.build(), recipient.build(), document.getPayload());
    }

    public byte[] getManifestContent() {
        ObjectFactory objectFactory = new ObjectFactory();
        return marshallObject(objectFactory.createManifest(manifest));
    }

    public byte[] getRecipientsContent() {
        ObjectFactory objectFactory = new ObjectFactory();
        ArrayOfRecipient arrayOfRecipient = objectFactory.createArrayOfRecipient();
        arrayOfRecipient.getRecipient().add(recipient);
        return marshallObject(objectFactory.createArrayOfRecipient(arrayOfRecipient));
    }

    public byte[] getPayload() {
        no.difi.meldingsutveksling.domain.sbdh.ObjectFactory objectFactory = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory();
        return marshallObject(objectFactory.createStandardBusinessDocument(document));
    }

    /**
     * Writes the Altinn package as a Zip file
     * @param outputStream where the Zip file is written
     * @throws IOException
     */
    public void write(OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        zipContent(zipOutputStream, "manifest.xml", getManifestContent());

        zipContent(zipOutputStream, "recipients.xml", getRecipientsContent());

        zipContent(zipOutputStream, "content.xml", getPayload());

        zipOutputStream.finish();
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    private void zipContent(ZipOutputStream zipOutputStream, String fileName, byte[] fileContent) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        zipOutputStream.write(fileContent);
        zipOutputStream.closeEntry();
    }

    private byte[] marshallObject(Object object) {
        try {

            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            marshaller.marshal(object, outputStream);
            return outputStream.toByteArray();
        } catch (JAXBException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

}
