package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;

@Data
@AllArgsConstructor(staticName = "of")
public class ServiceRecordWrapper {
    private ServiceRecord serviceRecord;
    private List<ServiceIdentifier> failedServiceIdentifiers;
}
