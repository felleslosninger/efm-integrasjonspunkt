package no.difi.meldingsutveksling.serviceregistry;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static no.difi.meldingsutveksling.serviceregistry.SRMarkers.markerFrom;
import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.hasDocumentType;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryLookup {

    private final ServiceRegistryClient serviceRegistryClient;
    private final IntegrasjonspunktProperties properties;

    public ServiceRecord getServiceRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        return loadServiceRecord(parameter);
    }

    public List<ServiceRecord> getServiceRecords(SRParameter parameter) {
        try {
            return loadServiceRecords(parameter);
        } catch (ServiceRegistryLookupException e) {
            log.error(markerFrom(parameter), formatErrorMsg(parameter));
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public boolean isInServiceRegistry(Iso6523 identifier) {
        return !getServiceRecords(SRParameter.builder(identifier).build()).isEmpty();
    }

    public ServiceRecord getServiceRecord(SRParameter parameter, String documentType) throws ServiceRegistryLookupException {
        if (Strings.isNullOrEmpty(parameter.getProcess())) {
            throw new IllegalArgumentException("Cannot determine documentType with missing process parameter");
        }

        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> {
                    log.error(markerFrom(parameter, documentType), formatErrorMsg(parameter, documentType));
                    return new ServiceRegistryLookupException(formatErrorMsg(parameter, documentType));
                });
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

        return serviceRecord.orElseThrow(() -> {
            log.error(markerFrom(parameter), formatErrorMsg(parameter));
            return new ServiceRegistryLookupException(formatErrorMsg(parameter));
        });
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
    public InfoRecord getInfoRecord(PartnerIdentifier identifier) {
        try {
            return loadInfoRecord(SRParameter.builder(identifier).infoOnly(true).build());
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

    private String formatErrorMsg(SRParameter parameters) {
        return String.format("Error looking up service record with parameters: %s", parameters);
    }

    private String formatErrorMsg(SRParameter parameters, String doctype) {
        return String.format("Error looking up service record with document type '%s' and parameters: %s", doctype, parameters);
    }
}
