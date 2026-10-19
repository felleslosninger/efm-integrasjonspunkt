package no.difi.meldingsutveksling.dph.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.io.Resource;


@Data
public class SendBusinessDocumentInput {

    private StandardBusinessDocument sbd;
    private Resource encryptedAsic;
}
