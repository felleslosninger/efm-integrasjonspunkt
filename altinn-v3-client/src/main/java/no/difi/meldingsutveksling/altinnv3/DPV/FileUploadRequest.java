package no.difi.meldingsutveksling.altinnv3.DPV;

import lombok.Getter;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import org.springframework.core.io.Resource;

@Getter
public class FileUploadRequest {

    private final BusinessMessageFile businessMessageFile;
    private final Resource file;

    public FileUploadRequest(BusinessMessageFile businessMessageFile, Resource file) {
        this.businessMessageFile = businessMessageFile;
        this.file = file;
    }
}
