package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class ServiceRecord implements Serializable {

    public static final ServiceRecord EMPTY = new ServiceRecord("", "", "", "", "", "", "", false);
    private String serviceIdentifier;
    private String organisationNumber;
    private String pemCertificate;
    private String endPointURL;
    private String orgnrPostkasse;
    private String postkasseAdresse;
    private String epostAdresse;
    private String varslingsStatus;
    private String mobilnummer;
    private boolean fysiskPost;
    private boolean kanVarsles;

    public ServiceRecord(String serviceIdentifier, String organisationNumber, String pemCertificate, String endPointURL) {
        this.serviceIdentifier = serviceIdentifier;
        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.endPointURL = endPointURL;
    }

    private ServiceRecord(String serviceIdentifier, String organisationNumber, String pemCertificate
            , String endPointURL, String epostAdresse, String varslingsStatus, String mobilnummer, boolean fysiskPost) {
        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.endPointURL = endPointURL;
        this.serviceIdentifier = serviceIdentifier;
        this.epostAdresse = epostAdresse;
        this.varslingsStatus = varslingsStatus;
        this.mobilnummer = mobilnummer;
        this.fysiskPost = fysiskPost;
    }

    /** Needed by gson **/
    public ServiceRecord() {
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public String getPemCertificate() {
        return pemCertificate;
    }

    public void setPemCertificate(String pemCertificate) {
        this.pemCertificate = pemCertificate;
    }

    public String getEndPointURL() {
        return endPointURL;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public void setEndPointURL(String endPointURL) {
        this.endPointURL = endPointURL;
    }

    public String getOrgnrPostkasse() {
        return orgnrPostkasse;
    }

    public void setOrgnrPostkasse(String orgnrPostkasse) {
        this.orgnrPostkasse = orgnrPostkasse;
    }

    public String getPostkasseAdresse() {
        return postkasseAdresse;
    }

    public void setPostkasseAdresse(String postkasseAdresse) {
        this.postkasseAdresse = postkasseAdresse;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceIdentifier", serviceIdentifier)
                .add("organisationNumber", organisationNumber)
                .add("pemCertificate", pemCertificate)
                .add("endPointURL", endPointURL)
                .add("orgnrPostkasse", orgnrPostkasse)
                .add("postkasseAdresse", postkasseAdresse)
                .toString();
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRecord that = (ServiceRecord) o;
        return fysiskPost == that.fysiskPost &&
                kanVarsles == that.kanVarsles &&
                Objects.equal(serviceIdentifier, that.serviceIdentifier) &&
                Objects.equal(organisationNumber, that.organisationNumber) &&
                Objects.equal(pemCertificate, that.pemCertificate) &&
                Objects.equal(endPointURL, that.endPointURL) &&
                Objects.equal(orgnrPostkasse, that.orgnrPostkasse) &&
                Objects.equal(postkasseAdresse, that.postkasseAdresse) &&
                Objects.equal(epostAdresse, that.epostAdresse) &&
                Objects.equal(varslingsStatus, that.varslingsStatus) &&
                Objects.equal(mobilnummer, that.mobilnummer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceIdentifier, organisationNumber, pemCertificate, endPointURL, orgnrPostkasse, postkasseAdresse, epostAdresse, varslingsStatus, mobilnummer, fysiskPost, kanVarsles);
    }

    public String getEpostAdresse() {
        return epostAdresse;
    }

    public void setEpostAdresse(String epostAdresse) {
        this.epostAdresse = epostAdresse;
    }

    public void setVarslingsStatus(String varslingsStatus) {
        this.varslingsStatus = varslingsStatus;
    }

    public void setMobilnummer(String mobilnummer) {
        this.mobilnummer = mobilnummer;
    }

    public String getVarslingsStatus() {
        return varslingsStatus;
    }

    public String getMobilnummer() {
        return mobilnummer;
    }

    public boolean isFysiskPost() {
        return fysiskPost;
    }

    public void setFysiskPost(boolean fysiskPost) {
        this.fysiskPost = fysiskPost;
    }

    public boolean isKanVarsles() {
        return kanVarsles;
    }

    public void setKanVarsles(boolean kanVarsles) {
        this.kanVarsles = kanVarsles;
    }


}
