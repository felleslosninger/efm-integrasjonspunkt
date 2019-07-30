package no.difi.meldingsutveksling.serviceregistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.Process;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryLookup {

    private final ServiceRegistryClient serviceRegistryClient;
    private final IntegrasjonspunktProperties properties;

    /**
     * Method to find out which transport channel to use to send messages to given organization
     *
     * @param identifier of the receiver
     * @return a ServiceRecord if found. Otherwise an empty ServiceRecord is returned.
     */
    public ServiceRecord getServiceRecord(String identifier) throws ServiceRegistryLookupException {
        return getServiceRecord(SRParameter.builder(identifier).build());
    }

    public ServiceRecord getServiceRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        return loadServiceRecord(parameter);
    }

    public ServiceRecord getServiceRecord(String identifier, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        return getServiceRecord(SRParameter.builder(identifier).build(), serviceIdentifier);
    }

    public ServiceRecord getServiceRecord(SRParameter parameter, ServiceIdentifier serviceIdentifier) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        return serviceRecords.stream()
                .filter(isServiceIdentifier(serviceIdentifier)).findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record of type=%s not found for identifier=%s", serviceIdentifier, parameter.getIdentifier())));
    }

    List<ServiceRecord> getServiceRecords(String identifier) {
        return getServiceRecords(SRParameter.builder(identifier).build());
    }

    public List<ServiceRecord> getServiceRecords(SRParameter parameter) {
        try {
            return loadServiceRecords(parameter);
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public boolean isInServiceRegistry(String identifier) {
        return !getServiceRecords(identifier).isEmpty();
    }

    public String getDocumentIdentifier(SRParameter parameter, Process process, DocumentType documentType) throws ServiceRegistryLookupException {
        return getDocumentIdentifier(parameter, process.getValue(), documentType);
    }

    public String getDocumentIdentifier(SRParameter parameter, String process, DocumentType documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(parameter, process);
        return serviceRecords.stream()
                .flatMap(r -> r.getDocumentTypes().stream())
                .filter(documentType::fitsDocumentIdentifier)
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(
                        String.format("Standard not found for process '%s' and documentType '%s' for identifier '%s'",
                                process, documentType.getType(), parameter.getIdentifier())));

    }

    public ServiceRecord getServiceRecord(SRParameter parameter, String process, String documentType) throws ServiceRegistryLookupException {
        Set<ServiceRecord> serviceRecords = getServiceRecords(parameter, process);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", parameter.getIdentifier(), process)));
    }

    private Set<ServiceRecord> getServiceRecords(SRParameter parameter, String process) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        return serviceRecords.stream()
                .filter(isProcess(process))
                .collect(Collectors.toSet());
    }

    private ServiceRecord loadServiceRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);

        Optional<ServiceRecord> serviceRecord = serviceRecords.stream()
                .filter(r -> r.getService().getIdentifier() == ServiceIdentifier.DPI)
                .findFirst();

        if (!serviceRecord.isPresent()) {
            String defaultProcess = properties.getArkivmelding().getDefaultProcess();
            serviceRecord = serviceRecords.stream()
                    .filter(r -> r.getProcess().equals(defaultProcess))
                    .findFirst();
        }

        if (!serviceRecord.isPresent()) {
            serviceRecord = serviceRecords.stream()
                    .filter(r -> r.getService().getIdentifier() == ServiceIdentifier.DPE)
                    .findFirst();
        }

        return serviceRecord.orElseThrow(() -> new ServiceRegistryLookupException(String.format("Could not find service record for receiver '%s'", parameter.getIdentifier())));
    }

    private List<ServiceRecord> loadServiceRecords(SRParameter parameter) throws ServiceRegistryLookupException {
        return serviceRegistryClient.loadIdentifierResource(parameter).getServiceRecords();
    }

    /**
     * Method to fetch the info record for the given identifier
     *
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    public InfoRecord getInfoRecord(String identifier) {
        try {
            return loadInfoRecord(SRParameter.builder(identifier).build());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    private InfoRecord loadInfoRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        return serviceRegistryClient.loadIdentifierResource(parameter).getInfoRecord();
    }

    public String getSasKey() {
        return serviceRegistryClient.getSasKey();
    }
}
