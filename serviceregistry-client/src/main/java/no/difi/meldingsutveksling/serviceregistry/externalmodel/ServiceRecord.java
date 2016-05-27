package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import java.io.Serializable;

public class ServiceRecord implements Serializable {

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
}
