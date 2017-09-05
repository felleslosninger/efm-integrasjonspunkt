package no.difi.meldingsutveksling.serviceregistry;

import lombok.Data;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

@Data(staticConstructor = "of")
public class SRResponseWrapper {

    private final InfoRecord infoRecord;
    private final ServiceRecord serviceRecord;

}
