package no.difi.meldingsutveksling.serviceregistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord.hasDocumentType;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRegistryLookup {

    private final ServiceRegistryClient serviceRegistryClient;
    private final IntegrasjonspunktProperties properties;

    public ServiceRecord getReceiverServiceRecord(StandardBusinessDocument sbd) throws ServiceRegistryLookupException {
        BusinessMessage<?> businessMessage = sbd.getBusinessMessage();
        SRParameter.SRParameterBuilder parameterBuilder = SRParameter.builder(sbd.getReceiverIdentifier())
                .process(sbd.getProcess());
        sbd.getOptionalConversationId().ifPresent(parameterBuilder::conversationId);
        if (businessMessage.getSikkerhetsnivaa() != null) {
            parameterBuilder.securityLevel(businessMessage.getSikkerhetsnivaa());
        }

        return getReceiverServiceRecord(
                parameterBuilder.build(),
                sbd.getDocumentType());
    }


    public ServiceRecord getReceiverServiceRecord(SRParameter parameter) throws ServiceRegistryLookupException {
        return loadServiceRecord(parameter);
    }

    public List<ServiceRecord> getServiceRecords(SRParameter parameter) {
        try {
            return loadServiceRecords(parameter);
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

    public boolean isInServiceRegistry(String identifier) {
        return !getServiceRecords(SRParameter.builder(identifier).build()).isEmpty();
    }

    public String getDocumentIdentifier(SRParameter parameter, DocumentType documentType) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        return serviceRecords.stream()
                .flatMap(r -> r.getDocumentTypes().stream())
                .filter(documentType::fitsDocumentIdentifier)
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(
                        String.format("Standard not found for process '%s' and documentType '%s' for identifier '%s'",
                                parameter.getProcess(), documentType.getType(), parameter.getIdentifier())));

    }

    public ServiceRecord getReceiverServiceRecord(SRParameter parameter, String documentType) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        Predicate<ServiceRecord> hasBetaArkivmelding = s -> documentType.contains("arkivmelding55") && s.getDocumentTypes().contains("urn:no:difi:arkivmelding:xsd::arkivmelding");
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType).or(hasBetaArkivmelding))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", parameter.getIdentifier(), parameter.getProcess())));
    }

    public ServiceRecord getReceiverServiceRecord(SRParameter parameter, DocumentType documentType) throws ServiceRegistryLookupException {
        List<ServiceRecord> serviceRecords = loadServiceRecords(parameter);
        return serviceRecords.stream()
                .filter(hasDocumentType(documentType))
                .findFirst()
                .orElseThrow(() -> new ServiceRegistryLookupException(String.format("Service record for identifier=%s with process=%s not found", parameter.getIdentifier(), parameter.getProcess())));
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
        try {
            return serviceRegistryClient.loadIdentifierResource(parameter).getServiceRecords();
        } catch (ServiceRegistryLookupException e) {
            // Temporary hack to support old "arkivmelding" beta receivers - will be removed in a future update.
            // Integrasjonspunktet will in this case downgrade "arkivmelding" to beta format before sending.
            // See https://difino.atlassian.net/browse/MOVE-1952
            if (e instanceof NotFoundInServiceRegistryException &&
                    parameter.getProcess().contains(DocumentType.ARKIVMELDING.getType()) &&
                    parameter.getProcess().contains("ver5.5")) {
                parameter.setProcess(parameter.getProcess().replace("ver5.5", "ver1.0"));
                return serviceRegistryClient.loadIdentifierResource(parameter).getServiceRecords();
            }
            throw e;
        }
    }

    /**
     * Method to fetch the info record for the given identifier
     *
     * @param identifier of the receiver
     * @return an {@link InfoRecord} for the respective identifier
     */
    public InfoRecord getInfoRecord(String identifier) {
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
}
