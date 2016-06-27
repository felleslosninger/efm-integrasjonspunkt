package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

import static com.google.common.base.Objects.equal;
import static java.util.Objects.hash;

public class ServiceRecord implements Serializable {

    public static final ServiceRecord EMPTY = new ServiceRecord("", "", "", "");
    private String serviceIdentifier;
    private String organisationNumber;
    private String pemCertificate;
    private String endPointURL;

    public ServiceRecord(String serviceIdentifier, String organisationNumber, String pemCertificate
            , String endPointURL) {

        this.organisationNumber = organisationNumber;
        this.pemCertificate = pemCertificate;
        this.endPointURL = endPointURL;
        this.serviceIdentifier = serviceIdentifier;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceIdentifier", serviceIdentifier)
                .add("organisationNumber", organisationNumber)
                .add("pemCertificate", pemCertificate)
                .add("endPointURL", endPointURL)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRecord that = (ServiceRecord) o;
        return Objects.equal(serviceIdentifier, that.serviceIdentifier) &&
                Objects.equal(organisationNumber, that.organisationNumber) &&
                Objects.equal(pemCertificate, that.pemCertificate) &&
                Objects.equal(endPointURL, that.endPointURL);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(serviceIdentifier, organisationNumber, pemCertificate, endPointURL);
    }
}
