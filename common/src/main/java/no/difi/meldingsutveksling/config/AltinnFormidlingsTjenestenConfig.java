package no.difi.meldingsutveksling.config;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
public class AltinnFormidlingsTjenestenConfig {

    /**
     * System user username for altinn.
     */
    private String username;
    /**
     * System user password for altinn;
     */
    private String password;
    /**
     * TODO: descrive
     */
    private String externalServiceCode;
    /**
     * TODO: descrive
     */
    private int externalServiceEditionCode;
    private String streamingserviceUrl;
    private String brokerserviceUrl;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExternalServiceCode() {
        return externalServiceCode;
    }

    public void setExternalServiceCode(String externalServiceCode) {
        this.externalServiceCode = externalServiceCode;
    }

    public int getExternalServiceEditionCode() {
        return externalServiceEditionCode;
    }

    public void setExternalServiceEditionCode(int externalServiceEditionCode) {
        this.externalServiceEditionCode = externalServiceEditionCode;
    }

    public String getStreamingserviceUrl() {
        return streamingserviceUrl;
    }

    public void setStreamingserviceUrl(String streamingserviceUrl) {
        this.streamingserviceUrl = streamingserviceUrl;
    }

    public String getBrokerserviceUrl() {
        return brokerserviceUrl;
    }

    public void setBrokerserviceUrl(String brokerserviceUrl) {
        this.brokerserviceUrl = brokerserviceUrl;
    }

}
