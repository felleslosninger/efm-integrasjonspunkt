package no.difi.meldingsutveksling.web;

import java.util.List;

public interface FrontendFunctionality {

    /**
     * How does it work?
     * <br>
     * This interface is the glue between the frontend and backend.  It gathers all functions needed for
     * the web-ui.  It has two implementations, a real one for production and a fake one for development.
     * <br>
     * The FrontendFunctionalityFaker.java is used in development mode and is part of the web module.
     * It does not have any dependencies on other modules, databases, queues or other external systems.
     * The fake implementation will simulate the behavior of a real, so that the web module can be
     * developed easily with hot-reload enabled for productivity.
     * <br>
     * The FrontendFunctionalityImpl.java is the real implementation and resides in the integrasjonspunkt
     * module.  It will be used in production and have real functionality with configuration and external
     * systems.
     * <br>
     * To select between the two implementations, we use the property `use.frontend.faker=true`.
     * Beans of the correct type are then produced using @ConditionalOnProperty and this property.
     * See {@link no.difi.meldingsutveksling.config.IntegrasjonspunktBeans#frontendFunctionality()}
     * and {@link no.difi.meldingsutveksling.web.ThymeleafApplication#frontendFunctionality()}
     */

    record Version(String yours, String latest, boolean outdated) {};
    record Property(String key, String value, String description) {}

    default boolean isFake() {
        return false; // signal that this is a real implementation
    }

    String getOrganizationNumber();

    Version getIntegrasjonspunktVersion();

    List<String> getChannelsEnabled();

    List<Property> configuration();

    List<Property> configurationDPO();

    List<Property> configurationDPV();

    List<Property> configurationDPI();

}
