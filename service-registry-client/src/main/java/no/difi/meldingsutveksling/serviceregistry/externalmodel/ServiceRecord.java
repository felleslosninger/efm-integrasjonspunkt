package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Data
public class ServiceRecord {

    public static final ServiceRecord EMPTY = new ServiceRecord();

    private String organisationNumber;
    private String pemCertificate;
    private String orgnrPostkasse;
    private String postkasseAdresse;
    private String epostAdresse;
    private String mobilnummer;
    private boolean fysiskPost;
    private boolean kanVarsles;
    private PostAddress postAddress;
    private PostAddress returnAddress;
    private List<String> dpeCapabilities;
    private String process;
    private List<String> documentTypes;
    private Service service;

    public ServiceRecord(ServiceIdentifier serviceIdentifier, String organisationNumber, String pemCertificate, String endPointURL) {
        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.service = new Service(serviceIdentifier, endPointURL);
    }

    public ServiceRecord() {
        this.organisationNumber = "";
        this.pemCertificate = "";
        this.epostAdresse = "";
        this.mobilnummer = "";
        this.fysiskPost = false;
        this.postAddress = PostAddress.EMPTY;
        this.returnAddress = PostAddress.EMPTY;
        this.service = new Service();
    }

    public static Predicate<ServiceRecord> isServiceIdentifier(ServiceIdentifier identifier) {
        return s -> s != null && s.getService().getIdentifier().equals(identifier);
    }

    public static Predicate<ServiceRecord> isProcess(String process) {
        return s -> s != null && s.getProcess().equals(process);
    }

    @JsonIgnore
    public ServiceIdentifier getServiceIdentifier() {
        return getService().getIdentifier();
    }

    public Optional<String> getStandard(DocumentType documentType) {
        final String suffix = "::" + documentType.getType();

        return documentTypes.stream()
                .filter(p -> p.endsWith(suffix))
                .findAny();
    }
}
