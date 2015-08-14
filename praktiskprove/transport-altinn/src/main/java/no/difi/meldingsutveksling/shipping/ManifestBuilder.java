package no.difi.meldingsutveksling.shipping;

import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;

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

    public BrokerServiceManifest build() {
        BrokerServiceManifest manifest = new BrokerServiceManifest();
        manifest.setReportee(partyNumber);
        manifest.setSendersReference(senderReference);

        BrokerServiceManifest.FileList fileList = new BrokerServiceManifest.FileList();
        BrokerServiceManifest.FileList.File file = new BrokerServiceManifest.FileList.File();
        file.setFileName(CONTENT_FILE_NAME);
        fileList.getFile().add(file);
        manifest.setFileList(fileList);

        return manifest;
    }
}
