package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor(staticName = "of")
public class ServiceRecordWrapper {
    private ServiceRecord serviceRecord;
    private List<ServiceIdentifier> failedServiceIdentifiers;
    private Map<ServiceIdentifier, Integer> securitylevels;
}
