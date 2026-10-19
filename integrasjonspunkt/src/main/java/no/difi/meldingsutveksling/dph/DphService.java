package no.difi.meldingsutveksling.dph;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

@RequiredArgsConstructor
public class DphService {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public Iso6523 getOnBehalfOf(NhnIdentifier nhnIdentifier) {
        return Iso6523.of(ICD.NO_ORG, getInfoRecord(nhnIdentifier).getOrganizationNumber());
    }

    public InfoRecord getInfoRecord(NhnIdentifier nhnIdentifier) {
        return serviceRegistryLookup.getInfoRecord(nhnIdentifier.getIdentifier());
    }
}
