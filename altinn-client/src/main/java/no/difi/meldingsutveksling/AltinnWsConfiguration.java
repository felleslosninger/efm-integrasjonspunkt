package no.difi.meldingsutveksling;

import java.net.URL;

public class AltinnWsConfiguration {
    private URL streamingServiceUrl;
    private URL brokerServiceUrl;
    private String username;
    private String password;

    public URL getStreamingServiceUrl() {
        return streamingServiceUrl;
    }

    public URL getBrokerServiceUrl() {
        return brokerServiceUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private AltinnWsConfiguration() {
    }

    public static class Builder {
        AltinnWsConfiguration configuration;

        public Builder() {
            configuration = new AltinnWsConfiguration();
        }

        public Builder withStreamingServiceUrl(URL url) {
            configuration.streamingServiceUrl = url;
            return this;
        }

        public Builder withBrokerServiceUrl(URL url) {
            configuration.brokerServiceUrl = url;
            return this;
        }

        public Builder withUsername(String username) {
            configuration.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            configuration.password = password;
            return this;
        }

        public AltinnWsConfiguration build() {
            return configuration;
        }
    }
}
