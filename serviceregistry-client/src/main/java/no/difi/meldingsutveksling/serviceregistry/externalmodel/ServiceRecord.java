package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import java.io.Serializable;

public class ServiceRecord implements Serializable {

    public static final ServiceRecord EMPTY = new ServiceRecord("", "", "", "", "");
    private String serviceIdentifier;
    private String organisationNumber;
    private String x509Certificate;
    private String payloadIdentifier;
    private String endPointURL;

    public ServiceRecord(String serviceIdentifier, String organisationNumber, String x509Certificate
            , String payloadIdentifier, String endPointURL) {

        this.organisationNumber = organisationNumber;
        this.x509Certificate = x509Certificate;
        this.payloadIdentifier = payloadIdentifier;
        this.endPointURL = endPointURL;
        this.serviceIdentifier = serviceIdentifier;
    }

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

    public String getPayloadIdentifier() {
        return payloadIdentifier;
    }

    public void setPayloadIdentifier(String payloadIdentifier) {
        this.payloadIdentifier = payloadIdentifier;
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
        return "ServiceRecord{" +
                "serviceIdentifier='" + serviceIdentifier + '\'' +
                ", organisationNumber='" + organisationNumber + '\'' +
                ", x509Certificate='" + x509Certificate + '\'' +
                ", payloadIdentifier='" + payloadIdentifier + '\'' +
                ", endPointURL='" + endPointURL + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceRecord that = (ServiceRecord) o;

        if (serviceIdentifier != null ? !serviceIdentifier.equals(that.serviceIdentifier) : that.serviceIdentifier != null)
            return false;
        if (organisationNumber != null ? !organisationNumber.equals(that.organisationNumber) : that.organisationNumber != null)
            return false;
        if (x509Certificate != null ? !x509Certificate.equals(that.x509Certificate) : that.x509Certificate != null)
            return false;
        if (payloadIdentifier != null ? !payloadIdentifier.equals(that.payloadIdentifier) : that.payloadIdentifier != null)
            return false;
        return endPointURL != null ? endPointURL.equals(that.endPointURL) : that.endPointURL == null;

    }

    @Override
    public int hashCode() {
        int result = serviceIdentifier != null ? serviceIdentifier.hashCode() : 0;
        result = 31 * result + (organisationNumber != null ? organisationNumber.hashCode() : 0);
        result = 31 * result + (x509Certificate != null ? x509Certificate.hashCode() : 0);
        result = 31 * result + (payloadIdentifier != null ? payloadIdentifier.hashCode() : 0);
        result = 31 * result + (endPointURL != null ? endPointURL.hashCode() : 0);
        return result;
    }
}
