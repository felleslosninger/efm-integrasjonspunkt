package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentifierResource {

    private InfoRecord infoRecord;
    private List<ServiceRecord> serviceRecords;
}
