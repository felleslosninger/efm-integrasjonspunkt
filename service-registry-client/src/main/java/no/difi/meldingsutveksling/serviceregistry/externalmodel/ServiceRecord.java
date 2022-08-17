package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import org.springframework.util.StringUtils;
import sun.security.provider.X509Factory;

import java.util.List;
import java.util.function.Predicate;

@Data
public class ServiceRecord {

    public static final ServiceRecord EMPTY = new ServiceRecord();

    @JsonProperty("organisationNumber")
    private PartnerIdentifier identifier;
    private String pemCertificate;
    private String orgnrPostkasse;
    private String postkasseAdresse;
    private String epostAdresse;
    private String mobilnummer;
    private boolean fysiskPost;
    private boolean kanVarsles;
    private PostAddress postAddress;
    private PostAddress returnAddress;
    private String process;
    private List<String> documentTypes;
    private Service service;

    public ServiceRecord(ServiceIdentifier serviceIdentifier, PartnerIdentifier identifier, String pemCertificate, String endPointURL) {
        this.identifier = identifier;
        this.pemCertificate = pemCertificate;
        this.service = new Service(serviceIdentifier, endPointURL);
    }

    public ServiceRecord() {
        this.identifier = Iso6523.of(ICD.NO_ORG,"12345678");
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

    public static Predicate<ServiceRecord> hasDocumentType(String documentType) {
        return s -> s != null && s.getDocumentTypes().contains(documentType);
    }

    @JsonIgnore
    public ServiceIdentifier getServiceIdentifier() {
        return getService().getIdentifier();
    }

    public String getPemCertificate() {
        if (StringUtils.hasText(pemCertificate) && !pemCertificate.contains(X509Factory.BEGIN_CERT)) {
            return String.format("%s\n%s\n%s\n", X509Factory.BEGIN_CERT, pemCertificate, X509Factory.END_CERT);
        }
        return pemCertificate;
    }

}
