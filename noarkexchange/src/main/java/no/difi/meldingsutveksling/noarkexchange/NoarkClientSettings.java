package no.difi.meldingsutveksling.noarkexchange;

public class NoarkClientSettings {

    private final String endpointUrl;
    private final String userName;
    private final String password;
    private final String domain;


    public NoarkClientSettings(String password, String userName, String endpointUrl) {
        this.password = password;
        this.userName = userName;
        this.endpointUrl = endpointUrl;
        this.domain = null;
    }

    public NoarkClientSettings(String endpointUrl, String userName, String password, String domain) {
        this.endpointUrl = endpointUrl;
        this.userName = userName;
        this.password = password;
        this.domain = domain;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }
}
