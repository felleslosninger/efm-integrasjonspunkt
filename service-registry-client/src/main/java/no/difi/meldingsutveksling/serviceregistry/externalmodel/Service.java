package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

@Data
public class Service {

    public static final Service EMPTY = new Service();

    private ServiceIdentifier identifier;
    private String endpointUrl;
    private String serviceCode;
    private String serviceEditionCode;
    private Integer securityLevel;
    private String herdId1;
    private String herId2;

    public Service(ServiceIdentifier identifier, String endpointUrl) {
        this.identifier = identifier;
        this.endpointUrl = endpointUrl;
    }

    public Service() {
        this.endpointUrl = "";
    }
}
