package no.difi.meldingsutveksling.serviceregistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IdentifierRelayController {

    private final ServiceRegistryLookup serviceRegistryLookup;

    @GetMapping(value = "/servicerecord/{identifier}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getServiceRecord(@PathVariable("identifier") PartnerIdentifier identifier) {
        ServiceRecord serviceRecord = null;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(identifier).build());
        } catch (ServiceRegistryLookupException e) {
            log.error("Error while looking up service record for {}", identifier, e);
            return ResponseEntity.notFound().build();
        }

        List<ServiceRecord> serviceRecords = serviceRegistryLookup.getServiceRecords(SRParameter.builder(identifier).build());
        if (serviceRecords.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(identifier);
        return ResponseEntity.ok(SRResponseWrapper.of(infoRecord, serviceRecord, serviceRecords));
    }

}
