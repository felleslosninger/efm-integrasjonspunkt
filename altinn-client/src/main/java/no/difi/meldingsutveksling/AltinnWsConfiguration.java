package no.difi.meldingsutveksling;

import java.net.MalformedURLException;
import java.net.URL;
import org.springframework.context.ApplicationContext;

public class AltinnWsConfiguration {

    private URL streamingServiceUrl;
    private URL brokerServiceUrl;
    private String username;
    private String password;
    private String externalServiceCode;
    private int externalServiceEditionCode;

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

    public static AltinnWsConfiguration fromConfiguration(String hostName, ApplicationContext context) {
        AltinnConfig config = context.getBean(AltinnConfig.class);
        URL streamingserviceUrl = createUrl(hostName + config.getStreamingserviceUrl());
        URL brokerserviceUrl = createUrl(hostName + config.getBrokerserviceUrl());

        return new Builder()
                .withUsername(config.getUsername())
                .withPassword(config.getPassword())
                .withStreamingServiceUrl(streamingserviceUrl)
                .withBrokerServiceUrl(brokerserviceUrl)
                .withExternalServiceCode(config.getExternalServiceCode())
                .withExternalServiceEditionCode(config.getExternalServiceEditionCode())
                .build();
    }

    private static URL createUrl(String url) {
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            throw new AltinnWsConfigurationException("The configured URL is invalid ", e);
        }
        return u;
    }

    public String getExternalServiceCode() {
        return externalServiceCode;
    }

    public int getExternalServiceEditionCode() {
        return externalServiceEditionCode;
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

        public Builder withExternalServiceCode(String externalServiceCode) {
            configuration.externalServiceCode = externalServiceCode;
            return this;
        }

        public Builder withExternalServiceEditionCode(int externalServiceEditionCode) {
            configuration.externalServiceEditionCode = externalServiceEditionCode;
            return this;
        }
    }

    private static class AltinnWsConfigurationException extends RuntimeException {

        public AltinnWsConfigurationException(String message, Exception e) {
            super(message, e);
        }
    }

}
