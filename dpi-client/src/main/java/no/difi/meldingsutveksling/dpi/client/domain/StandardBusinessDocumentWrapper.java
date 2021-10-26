package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Data
@NoArgsConstructor
public class StandardBusinessDocumentWrapper {

    private StandardBusinessDocument standardBusinessDocument;

    public StandardBusinessDocumentWrapper(StandardBusinessDocument standardBusinessDocument) {
        this.standardBusinessDocument = standardBusinessDocument;
    }
}
