package no.difi.meldingsutveksling.domain.capabilities;

import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;

@Data
public class Capability {

    private String process;
    private ServiceIdentifier serviceIdentifier;
    private PostalAddress postAddress;
    private PostalAddress returnAddress;
    private List<DocumentType> documentTypes;
    private DigitalPostAddress digitalPostAddress;

}
