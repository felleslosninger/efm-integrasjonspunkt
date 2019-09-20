package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServiceIdentifierService {

    private final IntegrasjonspunktProperties properties;

    public boolean isEnabled(ServiceIdentifier serviceIdentifier) {
        switch (serviceIdentifier) {
            case DPO:
                return properties.getFeature().isEnableDPO();
            case DPE:
                return properties.getFeature().isEnableDPE();
            case DPI:
                return properties.getFeature().isEnableDPI();
            case DPF:
                return properties.getFeature().isEnableDPF();
            case DPV:
                return properties.getFeature().isEnableDPV();
            default:
                return false;
        }
    }
}
