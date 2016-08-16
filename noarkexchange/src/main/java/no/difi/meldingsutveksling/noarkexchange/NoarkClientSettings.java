package no.difi.meldingsutveksling.noarkexchange;

/**
 * Contains settings needed to send messages to Noark.
 * Can also be used to create Spring WS template factory
 */
public class NoarkClientSettings {

    private final String endpointUrl;
    private final String userName;
    private final String password;
    private final String domain;

    public NoarkClientSettings(String endpointUrl, String userName, String password) {
        this.endpointUrl = endpointUrl;
        this.userName = userName;
        this.password = password;
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

    public WebServiceTemplateFactory createTemplateFactory() {
        if(hasDomain() && hasUsername()){
            return new NtlmTemplateFactory(this);
        } else {
            return new DefaultTemplateFactory();
        }
    }

    private boolean hasDomain() {
        return domain != null && !domain.isEmpty();
    }

    private boolean hasUsername() {
        return userName != null && !userName.isEmpty();
    }
}
