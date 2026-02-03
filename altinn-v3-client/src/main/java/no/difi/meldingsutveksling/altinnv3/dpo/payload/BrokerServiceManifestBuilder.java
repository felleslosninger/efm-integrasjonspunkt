package no.difi.meldingsutveksling.altinnv3.dpo.payload;

import no.altinn.schema.services.serviceengine.broker._2015._06.BrokerServiceManifest;

/**
 * Builder for Manifest.xml need by Altinn formidlingstjeneste
 */
public class BrokerServiceManifestBuilder {
    private String partyNumber;
    private String senderReference;
    private ExternalServiceBuilder.ExternalService externalService;
    private String fileName;

    /**
     * @param partyNumber is an organisation number or a person number
     * @return the builder according to the builder pattern
     */
    public BrokerServiceManifestBuilder withSender(String partyNumber) {
        this.partyNumber = partyNumber;
        return this;
    }

    public BrokerServiceManifestBuilder withSenderReference(String senderReference) {
        this.senderReference = senderReference;
        return this;
    }

    public BrokerServiceManifestBuilder withExternalService(ExternalServiceBuilder.ExternalService externalService) {
        this.externalService = externalService;
        return this;
    }

    public BrokerServiceManifestBuilder withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public BrokerServiceManifest build() {
        BrokerServiceManifest manifest = new BrokerServiceManifest();
        manifest.setReportee(partyNumber);
        manifest.setSendersReference(senderReference);
        manifest.setExternalServiceCode(externalService.getExternalServiceCode());
        manifest.setExternalServiceEditionCode(externalService.getExternalServiceEditionCode());

        BrokerServiceManifest.FileList fileList = new BrokerServiceManifest.FileList();
        BrokerServiceManifest.FileList.File file = new BrokerServiceManifest.FileList.File();
        file.setFileName(fileName);
        fileList.getFile().add(file);
        manifest.setFileList(fileList);

        return manifest;
    }
}
