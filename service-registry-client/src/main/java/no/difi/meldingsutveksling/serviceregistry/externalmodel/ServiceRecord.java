package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

import static com.google.common.base.Objects.equal;
import static java.util.Objects.hash;

public class ServiceRecord implements Serializable {

    public static final ServiceRecord EMPTY = new ServiceRecord("", "", "", "");
    private String serviceIdentifier;
    private String organisationNumber;
    private String x509Certificate;
    private String endPointURL;

    public ServiceRecord(String serviceIdentifier, String organisationNumber, String x509Certificate
            , String endPointURL) {

        this.organisationNumber = organisationNumber;
        this.x509Certificate = x509Certificate;
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

    public String getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(String x509Certificate) {
        this.x509Certificate = x509Certificate;
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
                .add("organizationNumber", organisationNumber)
                .add("X509Certificate", x509Certificate)
                .add("endpointUrl", endPointURL)
                .add("serviceIdentifier", serviceIdentifier)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ServiceRecord) {
            ServiceRecord other = (ServiceRecord) o;

            return equal(serviceIdentifier, other.getServiceIdentifier())
                    && equal(organisationNumber, other.getOrganisationNumber())
                    && equal(x509Certificate, other.getX509Certificate())
                    && equal(getEndPointURL(), other.getEndPointURL());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return hash(serviceIdentifier, organisationNumber, x509Certificate, endPointURL);
    }
}
