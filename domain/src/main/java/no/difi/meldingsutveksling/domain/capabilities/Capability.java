package no.difi.meldingsutveksling.domain.capabilities;

import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;

@Data
public class Capability {

    private String process;
    private ServiceIdentifier serviceIdentifier;
    private List<DocumentType> documentTypes;
}
