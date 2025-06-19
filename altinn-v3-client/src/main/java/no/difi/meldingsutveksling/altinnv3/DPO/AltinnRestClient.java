package no.difi.meldingsutveksling.altinnv3.DPO;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.List;

public class AltinnRestClient {

    public AltinnRestClient(IntegrasjonspunktProperties properties) {
        //
    }

    public boolean checkIfAvailableFiles(String o) {
        return false;
    }

    public List<FileReference> availableFiles(String o) {
        return null;

    }

    public AltinnPackage download(DownloadRequest request) {
        return null;
    }

    public void confirmDownload(DownloadRequest request) {

    }

}
