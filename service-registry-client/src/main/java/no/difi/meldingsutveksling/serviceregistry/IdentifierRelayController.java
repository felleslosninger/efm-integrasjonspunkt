package no.difi.meldingsutveksling.serviceregistry;

import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecordWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IdentifierRelayController {

    @Autowired
    private ServiceRegistryLookup serviceRegistryLookup;

    @RequestMapping(value = "/servicerecord/{identifier}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getServiceRecord(@PathVariable("identifier") String identifier) {
        ServiceRecordWrapper serviceRecord = serviceRegistryLookup.getServiceRecord(identifier);

        List<ServiceRecord> serviceRecords = serviceRegistryLookup.getServiceRecords(identifier);
        if (serviceRecords.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(identifier);
        return ResponseEntity.ok(SRResponseWrapper.of(infoRecord, serviceRecord.getServiceRecord(), serviceRecords));
    }

}
