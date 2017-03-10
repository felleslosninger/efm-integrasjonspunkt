package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;
import java.util.function.Predicate;

@Data
public class ServiceRecord {

    public static final ServiceRecord EMPTY = new ServiceRecord();
    private ServiceIdentifier serviceIdentifier;
    private String organisationNumber;
    private String pemCertificate;
    private String endPointURL;
    private String serviceCode;
    private String serviceEditionCode;
    private String orgnrPostkasse;
    private String postkasseAdresse;
    private String epostAdresse;
    private String mobilnummer;
    private boolean fysiskPost;
    private boolean kanVarsles;
    private PostAddress postAddress;
    private PostAddress returnAddress;
    private List<String> dpeCapabilities;

    public ServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String pemCertificate, String endPointURL) {
        this.serviceIdentifier = serviceIdentifier;
        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.endPointURL = endPointURL;
    }

    public ServiceRecord() {
        this.organisationNumber = "";
        this.pemCertificate = "";
        this.endPointURL = "";
        this.epostAdresse = "";
        this.mobilnummer = "";
        this.fysiskPost = false;
        this.postAddress = PostAddress.EMPTY;
        this.returnAddress = PostAddress.EMPTY;
    }

    public static Predicate<ServiceRecord> isServiceIdentifier(ServiceIdentifier identifier) {
        return s -> s != null && s.getServiceIdentifier().equals(identifier);
    }

}
