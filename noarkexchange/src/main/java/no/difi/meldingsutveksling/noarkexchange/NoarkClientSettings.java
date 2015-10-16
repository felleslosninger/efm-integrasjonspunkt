package no.difi.meldingsutveksling.noarkexchange;

public class NoarkClientSettings {
    private final String endpointUrl;
    private final String userName;
    private final String password;

    public NoarkClientSettings(String endpointUrl, String userName, String password) {
        this.endpointUrl = endpointUrl;
        this.userName = userName;
        this.password = password;
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
}
