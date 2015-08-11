package no.difi.meldingsutveksling.shipping;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ArrayOfFile;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.File;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Manifest;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.ObjectFactory;

/**
 * Builder for Manifest.xml need by Altinn formidlingstjeneste
 */
public class ManifestBuilder {
    public static final String CONTENT_FILE_NAME = "content.xml";
    private String partyNumber;
    private String senderReference;

    /**
     *
     * @param partyNumber is an organisation number or a person number
     * @return the builder according to the builder pattern
     */
    public ManifestBuilder withSender(String partyNumber) {
        this.partyNumber = partyNumber;
        return this;
    }

    public ManifestBuilder withSenderReference(String senderReference) {
        this.senderReference = senderReference;
        return this;
    }

    public Manifest build() {
        Manifest manifest = new Manifest();
        manifest.setReportee(partyNumber);
        manifest.setSendersReference(senderReference);

        ObjectFactory objectFactory = new ObjectFactory();
        File file = objectFactory.createFile();
        file.setFileName(CONTENT_FILE_NAME);
        ArrayOfFile arrayOfFile = objectFactory.createArrayOfFile();
        arrayOfFile.getFile().add(file);
        manifest.setFileList(objectFactory.createManifestFileList(arrayOfFile));

        return manifest;
    }
}
