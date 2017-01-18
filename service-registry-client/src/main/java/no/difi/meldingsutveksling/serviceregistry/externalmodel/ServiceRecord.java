package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class ServiceRecord {

    public static final ServiceRecord EMPTY = new ServiceRecord();
    private String serviceIdentifier;
    private String organisationNumber;
    private String pemCertificate;
    private String endPointURL;
    private String orgnrPostkasse;
    private String postkasseAdresse;
    private String epostAdresse;
    private String mobilnummer;
    private boolean fysiskPost;
    private boolean kanVarsles;
    private PostAddress postAddress;
    private PostAddress returnAddress;

    public ServiceRecord(String serviceIdentifier, String organisationNumber, String pemCertificate, String endPointURL) {
        this.serviceIdentifier = serviceIdentifier;
        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.endPointURL = endPointURL;
    }

    public ServiceRecord() {
        this.organisationNumber = "";
        this.pemCertificate = "";
        this.endPointURL = "";
        this.serviceIdentifier = "";
        this.epostAdresse = "";
        this.mobilnummer = "";
        this.fysiskPost = false;
        this.postAddress = PostAddress.EMPTY;
        this.returnAddress = PostAddress.EMPTY;
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



    public String getEpostAdresse() {
        return epostAdresse;
    }

    public void setEpostAdresse(String epostAdresse) {
        this.epostAdresse = epostAdresse;
    }

    public void setMobilnummer(String mobilnummer) {
        this.mobilnummer = mobilnummer;
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

    public PostAddress getPostAddress() {
        return postAddress;
    }

    public void setPostAddress(PostAddress postAddress) {
        this.postAddress = postAddress;
    }

    public PostAddress getReturnAddress() {
        return returnAddress;
    }

    public void setReturnAddress(PostAddress returnAddress) {
        this.returnAddress = returnAddress;
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
                Objects.equal(mobilnummer, that.mobilnummer) &&
                Objects.equal(postAddress, that.postAddress) &&
                Objects.equal(returnAddress, that.returnAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceIdentifier, organisationNumber, pemCertificate, endPointURL, orgnrPostkasse, postkasseAdresse, epostAdresse, mobilnummer, fysiskPost, kanVarsles, postAddress, returnAddress);
    }
}
