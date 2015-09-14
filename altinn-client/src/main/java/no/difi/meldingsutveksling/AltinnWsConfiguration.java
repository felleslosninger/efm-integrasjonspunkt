package no.difi.meldingsutveksling;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.net.MalformedURLException;
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

    public static AltinnWsConfiguration fromProperties() {
        PropertiesConfiguration propertiesConfiguration;
        try {
            propertiesConfiguration = new PropertiesConfiguration("application.properties");
        } catch (ConfigurationException e) {
            throw new AltinnWsConfigurationException("Could not create configuration for the Altinn formidlingstjeneste client", e);
        }

        URL streamingserviceUrl = createUrl(propertiesConfiguration.getString("streamingservice.url"));
        URL brokerserviceUrl = createUrl(propertiesConfiguration.getString("brokerservice.url"));


        return new Builder()
                .withUsername(propertiesConfiguration.getString("altinn.username"))
                .withPassword(propertiesConfiguration.getString("altinn.password"))
                .withStreamingServiceUrl(streamingserviceUrl)
                .withBrokerServiceUrl(brokerserviceUrl)
                .build();
    }

    private static URL createUrl(String url) {
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            throw new AltinnWsConfigurationException("The configured URL is invalid", e);
        }
        return u;
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

    private static class AltinnWsConfigurationException extends RuntimeException {
        public AltinnWsConfigurationException(String message, Exception e) {
            super(message, e);
        }
    }

}
