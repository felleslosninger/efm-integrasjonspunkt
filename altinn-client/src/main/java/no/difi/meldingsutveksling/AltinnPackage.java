package no.difi.meldingsutveksling;

import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceRecipientList;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.sftp.BrokerServiceManifestBuilder;
import no.difi.meldingsutveksling.shipping.sftp.ExternalServiceBuilder;
import no.difi.meldingsutveksling.shipping.sftp.RecipientBuilder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Represents an Altinn package to be used with the formidlingstjeneste SFTP channel.
 * <p/>
 * Has factory methods of writing/reading from to zip files via input/output streams.
 */
public class AltinnPackage {
    private static JAXBContext ctx;
    private final BrokerServiceManifest manifest;
    private final BrokerServiceRecipientList recipient;
    private final EduDocument eduDocument;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{BrokerServiceManifest.class,
                    BrokerServiceRecipientList.class, EduDocument.class, Payload.class, Kvittering.class}, new HashMap());
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext", e);
        }
    }

    private AltinnPackage(BrokerServiceManifest manifest, BrokerServiceRecipientList recipient, EduDocument eduDocument) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.eduDocument = eduDocument;
    }

    public static AltinnPackage from(UploadRequest document) {
        BrokerServiceManifestBuilder manifest = new BrokerServiceManifestBuilder();
        manifest.withSender(document.getSender());
        manifest.withSenderReference(document.getSenderReference());
        manifest.withExternalService(
                new ExternalServiceBuilder()
                        .withExternalServiceCode("v3888")
                        .withExternalServiceEditionCode(new BigInteger("070515"))
                        .build());

        RecipientBuilder recipient = new RecipientBuilder(document.getReceiver());
        return new AltinnPackage(manifest.build(), recipient.build(), document.getPayload());
    }

    /**
     * Writes the Altinn package as a Zip file
     *
     * @param outputStream where the Zip file is written
     * @throws IOException
     */
    public void write(OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        zipOutputStream.putNextEntry(new ZipEntry("manifest.xml"));
        marshallObject(manifest, zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("recipients.xml"));
        marshallObject(recipient, zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("content.xml"));
        no.difi.meldingsutveksling.domain.sbdh.ObjectFactory objectFactory = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory();
        marshallObject(objectFactory.createStandardBusinessDocument(eduDocument), zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.finish();
        zipOutputStream.flush();
        zipOutputStream.close();
    }

    public static AltinnPackage from(File f) throws IOException, JAXBException {
        ZipFile zipFile = new ZipFile(f);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        BrokerServiceManifest manifest = null;
        BrokerServiceRecipientList recipientList = null;
        EduDocument eduDocument = null;

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().equals("manifest.xml")) {
                manifest = (BrokerServiceManifest) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
            } else if (zipEntry.getName().equals("recipients.xml")) {
                recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
            } else if (zipEntry.getName().equals("content.xml")) {
                Source source = new StreamSource(zipFile.getInputStream(zipEntry));
                eduDocument = unmarshaller.unmarshal(source, EduDocument.class).getValue();
            } else if (zipEntry.getName().equals("asic.zip")) {
                zipFile.getInputStream(zipEntry);
            }
        }

        return new AltinnPackage(manifest, recipientList, eduDocument);
    }

    public static AltinnPackage from(InputStream inputStream) throws IOException, JAXBException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);
        InputStream inputStreamProxy = new FilterInputStream(zipInputStream) {
            @Override
            public void close() throws IOException {
                // do nothing to avoid unmarshaller to close it before the Zip file is fully processed
            }
        };

        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        ArchiveEntry zipEntry;
        BrokerServiceManifest manifest = null;
        BrokerServiceRecipientList recipientList = null;
        EduDocument eduDocument = null;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (zipEntry.getName().equals("manifest.xml")) {
                manifest = (BrokerServiceManifest) unmarshaller.unmarshal(inputStreamProxy);
            } else if (zipEntry.getName().equals("recipients.xml")) {
                recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(inputStreamProxy);
            } else if (zipEntry.getName().equals("content.xml")) {
                Source source = new StreamSource(inputStreamProxy);
                eduDocument = unmarshaller.unmarshal(source, EduDocument.class).getValue();
            }
        }
        return new AltinnPackage(manifest, recipientList, eduDocument);
    }

    private void marshallObject(Object object, OutputStream outputStream) {
        try {
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(object, outputStream);
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    public EduDocument getEduDocument() {
        return eduDocument;
    }
}
