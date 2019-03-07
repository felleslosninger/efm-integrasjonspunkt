package no.difi.meldingsutveksling;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceRecipientList;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import no.difi.meldingsutveksling.shipping.sftp.BrokerServiceManifestBuilder;
import no.difi.meldingsutveksling.shipping.sftp.ExternalServiceBuilder;
import no.difi.meldingsutveksling.shipping.sftp.RecipientBuilder;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
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

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

/**
 * Represents an Altinn package to be used with the formidlingstjeneste SFTP channel.
 * <p/>
 * Has factory methods of writing/reading from to zip files via input/output streams.
 */
@Slf4j
public class AltinnPackage {
    private static JAXBContext ctx;
    private final BrokerServiceManifest manifest;
    private final BrokerServiceRecipientList recipient;
    private final StandardBusinessDocument sbd;
    private final InputStream asicInputStream;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{BrokerServiceManifest.class,
                    BrokerServiceRecipientList.class, StandardBusinessDocument.class, Payload.class, Kvittering.class}, new HashMap());
        } catch (JAXBException e) {
            throw new RuntimeException("Could not create JAXBContext", e);
        }
    }

    private AltinnPackage(BrokerServiceManifest manifest, BrokerServiceRecipientList recipient, StandardBusinessDocument sbd, InputStream asicInputStream) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.sbd = sbd;
        this.asicInputStream = asicInputStream;
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
        return new AltinnPackage(manifest.build(), recipient.build(), document.getPayload(), document.getAsicInputStream());
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


        if (sbd.getAny() instanceof Payload) {
            zipOutputStream.putNextEntry(new ZipEntry("content.xml"));
            no.difi.meldingsutveksling.domain.sbdh.ObjectFactory objectFactory = new no.difi.meldingsutveksling.domain.sbdh.ObjectFactory();
            marshallObject(objectFactory.createStandardBusinessDocument(sbd), zipOutputStream);
            zipOutputStream.closeEntry();

            Payload payload = (Payload) sbd.getAny();
            if (payload.getInputStream() != null) {
                zipOutputStream.putNextEntry(new ZipEntry(ASIC_FILE));
                IOUtils.copy(payload.getInputStream(), zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }

        if (sbd.getAny() instanceof BusinessMessage) {
            zipOutputStream.putNextEntry(new ZipEntry("sbd.json"));
            ObjectMapper om = new ObjectMapper();
            om.writeValue(zipOutputStream, sbd);
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry(ASIC_FILE));
            IOUtils.copy(this.asicInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }

        zipOutputStream.finish();
    }

    public static AltinnPackage from(File f, MessagePersister messagePersister) throws IOException, JAXBException {
        ZipFile zipFile = new ZipFile(f);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        BrokerServiceManifest manifest = null;
        BrokerServiceRecipientList recipientList = null;
        StandardBusinessDocument sbd = null;
        InputStream asicInputStream = null;
        long asicSize = 0;

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().equals("manifest.xml")) {
                manifest = (BrokerServiceManifest) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
            } else if (zipEntry.getName().equals("recipients.xml")) {
                recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
            } else if (zipEntry.getName().equals("sbd.json")) {
                ObjectMapper om = new ObjectMapper();
                sbd = om.readValue(zipFile.getInputStream(zipEntry), StandardBusinessDocument.class);
            } else if (zipEntry.getName().equals("content.xml")) {
                Source source = new StreamSource(zipFile.getInputStream(zipEntry));
                sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
            } else if (zipEntry.getName().equals(ASIC_FILE)) {
                asicInputStream = zipFile.getInputStream(zipEntry);
                asicSize = zipEntry.getSize();
            }
        }

        if (sbd == null) {
            throw new MeldingsUtvekslingRuntimeException("Altinn zip does not contain BestEdu document, cannot proceed");
        }
        if (asicInputStream != null) {
            messagePersister.writeStream(sbd.getConversationId(), ASIC_FILE, asicInputStream, asicSize);
        }
        zipFile.close();
        return new AltinnPackage(manifest, recipientList, sbd, null);
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
        StandardBusinessDocument sbd = null;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (zipEntry.getName().equals("manifest.xml")) {
                manifest = (BrokerServiceManifest) unmarshaller.unmarshal(inputStreamProxy);
            } else if (zipEntry.getName().equals("recipients.xml")) {
                recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(inputStreamProxy);
            } else if (zipEntry.getName().equals("content.xml")) {
                Source source = new StreamSource(inputStreamProxy);
                sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
            }
        }
        return new AltinnPackage(manifest, recipientList, sbd, null);
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

    public StandardBusinessDocument getSbd() {
        return sbd;
    }
}
