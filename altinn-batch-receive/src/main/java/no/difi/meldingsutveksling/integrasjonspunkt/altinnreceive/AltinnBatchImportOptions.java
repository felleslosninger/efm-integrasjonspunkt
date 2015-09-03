package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

/**
 */
public class AltinnBatchImportOptions {

    private String integrasjonspunktEndPointURL;
    private String organisationNumber;
    private int threadPoolSize;

    public AltinnBatchImportOptions(String integrasjonspunktEndPointURL, String organisationNumber, int threadPoolSize) {
        this.integrasjonspunktEndPointURL = integrasjonspunktEndPointURL;
        this.organisationNumber = organisationNumber;
        this.threadPoolSize = threadPoolSize;
    }

    public String getIntegrasjonspunktEndPointURL() {
        return integrasjonspunktEndPointURL;
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public int getThreadPoolSize() {
        return this.threadPoolSize;
    }
}
