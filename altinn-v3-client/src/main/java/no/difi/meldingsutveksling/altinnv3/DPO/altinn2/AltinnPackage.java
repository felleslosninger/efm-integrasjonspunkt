package no.difi.meldingsutveksling.altinnv3.DPO.altinn2;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;
import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceRecipientList;
import no.difi.meldingsutveksling.TmpFile;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.UploadRequest;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.sftp.BrokerServiceManifestBuilder;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.sftp.ExternalServiceBuilder;
import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.sftp.RecipientBuilder;
import no.difi.move.common.io.ResourceUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

/**
 * Represents an Altinn package to be used with the formidlingstjeneste SFTP channel.
 * Has factory methods of writing/reading from to zip files via input/output streams.
 */
@Slf4j
public class AltinnPackage {
    private static final String CONTENT_XML = "content.xml";
    private static final String RECIPIENTS_XML = "recipients.xml";
    private static final String MANIFEST_XML = "manifest.xml";

    private static final JAXBContext ctx;
    private final BrokerServiceManifest manifest;
    private final BrokerServiceRecipientList recipient;
    private final StandardBusinessDocument sbd;
    private final Resource asic;
    private final TmpFile tmpFile;

    static {
        try {
            ctx = JAXBContextFactory.createContext(new Class[]{BrokerServiceManifest.class,
                BrokerServiceRecipientList.class, StandardBusinessDocument.class}, new HashMap());
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not create JAXBContext", e);
        }
    }

    private AltinnPackage(BrokerServiceManifest manifest,
                          BrokerServiceRecipientList recipient,
                          StandardBusinessDocument sbd,
                          Resource asic,
                          TmpFile tmpFile) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.sbd = sbd;
        this.asic = asic;
        this.tmpFile = tmpFile;
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
        return new AltinnPackage(manifest, recipient, document.getPayload(), document.getAsic(), null);
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
     * @param writableResource where the Zip file is written
     * @param context          {@link ApplicationContext}
     * @throws IOException
     */
    public void write(WritableResource writableResource, ApplicationContext context) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(writableResource.getOutputStream())) {

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

                if (this.asic != null) {
                    zipOutputStream.putNextEntry(new ZipEntry(ASIC_FILE));
                    ResourceUtils.copy(this.asic, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }

            zipOutputStream.flush();
        }
    }

    public static AltinnPackage from(File f, ApplicationContext context) throws IOException, JAXBException {
        try (ZipFile zipFile = new ZipFile(f)) {
            Unmarshaller unmarshaller = ctx.createUnmarshaller();

            BrokerServiceManifest manifest = null;
            BrokerServiceRecipientList recipientList = null;
            StandardBusinessDocument sbd = null;
            TmpFile tmpAsicFile = null;
            Resource asic = null;

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
                        tmpAsicFile = TmpFile.create(zipFile.getInputStream(zipEntry));
                        asic = new FileSystemResource(tmpAsicFile.getFile());
                        break;
                    default:
                        log.info("Skipping file: {}", zipEntry.getName());
                }
            }

            if (sbd == null) {
                throw new MeldingsUtvekslingRuntimeException("Altinn zip does not contain BestEdu document, cannot proceed");
            }
            return new AltinnPackage(manifest, recipientList, sbd, asic, tmpAsicFile);
        }
    }

    public static AltinnPackage from(Resource altinnZip) throws IOException, JAXBException {
        try (ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(altinnZip.getInputStream())) {
            Unmarshaller unmarshaller = ctx.createUnmarshaller();

            ArchiveEntry zipEntry;
            BrokerServiceManifest manifest = null;
            BrokerServiceRecipientList recipientList = null;
            StandardBusinessDocument sbd = null;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                switch (zipEntry.getName()) {
                    case MANIFEST_XML:
                        manifest = (BrokerServiceManifest) unmarshaller.unmarshal(StreamUtils.nonClosing(zipInputStream));
                        break;
                    case RECIPIENTS_XML:
                        recipientList = (BrokerServiceRecipientList) unmarshaller.unmarshal(StreamUtils.nonClosing(zipInputStream));
                        break;
                    case CONTENT_XML:
                        Source source = new StreamSource(StreamUtils.nonClosing(zipInputStream));
                        sbd = unmarshaller.unmarshal(source, StandardBusinessDocument.class).getValue();
                        break;
                    default:
                        log.info("Skipping file: {}", zipEntry.getName());
                }
            }
            return new AltinnPackage(manifest, recipientList, sbd, null, null);
        }
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
        return this.sbd;
    }

    public Resource getAsic() {
        return this.asic;
    }

    public TmpFile getTmpFile() {
        return this.tmpFile;
    }
}
