package no.difi.meldingsutveksling;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceRecipientList;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
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
import org.springframework.context.ApplicationContext;

import javax.xml.bind.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

/**
 * Represents an Altinn package to be used with the formidlingstjeneste SFTP channel.
 * <p/>
 * Has factory methods of writing/reading from to zip files via input/output streams.
 */
@Slf4j
public class AltinnPackage {
    private static final String CONTENT_XML = "content.xml";
    private static final String RECIPIENTS_XML = "recipients.xml";
    private static final String MANIFEST_XML = "manifest.xml";

    private static JAXBContext ctx;
    private final BrokerServiceManifest manifest;
    private final BrokerServiceRecipientList recipient;
    private final StandardBusinessDocument sbd;
    private final InputStream asicInputStream;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{BrokerServiceManifest.class,
                    BrokerServiceRecipientList.class, StandardBusinessDocument.class, Kvittering.class}, new HashMap());
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not create JAXBContext", e);
        }
    }

    private AltinnPackage(BrokerServiceManifest manifest,
                          BrokerServiceRecipientList recipient,
                          StandardBusinessDocument sbd,
                          InputStream asicInputStream) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.sbd = sbd;
        this.asicInputStream = asicInputStream;
    }

    public static AltinnPackage from(UploadRequest document) {
        BrokerServiceManifest manifest = new BrokerServiceManifestBuilder()
                .withSender(document.getSender())
                .withSenderReference(document.getSenderReference())
                .withExternalService(
                        new ExternalServiceBuilder()
                                .withExternalServiceCode("v3888")
                                .withExternalServiceEditionCode(BigInteger.valueOf(70515))
                                .build())
                .withFileName(getFileName(document))
                .build();

        BrokerServiceRecipientList recipient = new RecipientBuilder(document.getReceiver()).build();
        return new AltinnPackage(manifest, recipient, document.getPayload(), document.getAsicInputStream());
    }

    private static String getFileName(UploadRequest document) {
        if (document.getPayload().getAny() instanceof BusinessMessage) {
            return SBD_FILE;
        }

        return CONTENT_XML;
    }

    /**
     * Writes the Altinn package as a Zip file
     *
     * @param outputStream where the Zip file is written
     */
    public void write(OutputStream outputStream, ApplicationContext context) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        zipOutputStream.putNextEntry(new ZipEntry(MANIFEST_XML));
        marshallObject(manifest, zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(RECIPIENTS_XML));
        marshallObject(recipient, zipOutputStream);
        zipOutputStream.closeEntry();

        if (sbd.getAny() instanceof BusinessMessage) {
            zipOutputStream.putNextEntry(new ZipEntry(SBD_FILE));
            ObjectMapper om = context.getBean(ObjectMapper.class);
            om.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
            om.writeValue(zipOutputStream, sbd);
            zipOutputStream.closeEntry();

            if (this.asicInputStream != null) {
                zipOutputStream.putNextEntry(new ZipEntry(ASIC_FILE));
                IOUtils.copy(this.asicInputStream, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }

        zipOutputStream.finish();
    }

    public static AltinnPackage from(File f, MessagePersister messagePersister, ApplicationContext context) throws IOException, JAXBException {
        try (ZipFile zipFile = new ZipFile(f)) {
            Unmarshaller unmarshaller = ctx.createUnmarshaller();

            BrokerServiceManifest manifest = null;
            BrokerServiceRecipientList recipientList = null;
            StandardBusinessDocument sbd = null;
            InputStream asicInputStream = null;
            long asicSize = 0;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                switch (zipEntry.getName()) {
                    case MANIFEST_XML:
                        manifest = (BrokerServiceManifest) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
                        break;
                    case RECIPIENTS_XML:
                        recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(zipFile.getInputStream(zipEntry));
                        break;
                    case SBD_FILE:
                        ObjectMapper om = context.getBean(ObjectMapper.class);
                        om.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
                        sbd = om.readValue(zipFile.getInputStream(zipEntry), StandardBusinessDocument.class);
                        break;
                    case CONTENT_XML:
                        Source source = new StreamSource(zipFile.getInputStream(zipEntry));
                        sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
                        break;
                    case ASIC_FILE:
                        asicInputStream = zipFile.getInputStream(zipEntry);
                        asicSize = zipEntry.getSize();
                        break;
                    default:
                        log.info("Skipping file: {}", zipEntry.getName());
                }
            }

            if (sbd == null) {
                throw new MeldingsUtvekslingRuntimeException("Altinn zip does not contain BestEdu document, cannot proceed");
            }
            if (asicInputStream != null) {
                messagePersister.writeStream(sbd.getDocumentId(), ASIC_FILE, asicInputStream, asicSize);
            }
            return new AltinnPackage(manifest, recipientList, sbd, null);
        }
    }

    public static AltinnPackage from(InputStream inputStream) throws IOException, JAXBException {
        ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(inputStream);
        InputStream inputStreamProxy = new FilterInputStream(zipInputStream) {
            @Override
            public void close() {
                // do nothing to avoid unmarshaller to close it before the Zip file is fully processed
            }
        };

        Unmarshaller unmarshaller = ctx.createUnmarshaller();

        ArchiveEntry zipEntry;
        BrokerServiceManifest manifest = null;
        BrokerServiceRecipientList recipientList = null;
        StandardBusinessDocument sbd = null;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            switch (zipEntry.getName()) {
                case MANIFEST_XML:
                    manifest = (BrokerServiceManifest) unmarshaller.unmarshal(inputStreamProxy);
                    break;
                case RECIPIENTS_XML:
                    recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(inputStreamProxy);
                    break;
                case CONTENT_XML:
                    Source source = new StreamSource(inputStreamProxy);
                    sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
                    break;
                default:
                    log.info("Skipping file: {}", zipEntry.getName());
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
            throw new MeldingsUtvekslingRuntimeException("Couldn't marshall object");
        }
    }

    public StandardBusinessDocument getSbd() {
        return sbd;
    }
}
