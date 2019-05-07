package no.difi.meldingsutveksling.serviceregistry;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class IdentifierRelayController {

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    @RequestMapping(value = "/servicerecord/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getServiceRecord(@PathVariable("identifier") String identifier) {
        ServiceRecord serviceRecord = null;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(identifier);
        } catch (ServiceRegistryLookupException e) {
            log.error("Error while looking up service record for {}", identifier, e);
            return ResponseEntity.notFound().build();
        }

        List<ServiceRecord> serviceRecords = serviceRegistryLookup.getServiceRecords(identifier);
        if (serviceRecords.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(identifier);
        return ResponseEntity.ok(SRResponseWrapper.of(infoRecord, serviceRecord, serviceRecords));
    }

}
