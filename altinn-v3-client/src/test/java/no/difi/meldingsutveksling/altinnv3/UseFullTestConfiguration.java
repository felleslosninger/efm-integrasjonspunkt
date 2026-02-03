package no.difi.meldingsutveksling.altinnv3;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The @UseFullTestConfiguration is a custom meta-annotation to streamline test configuration.
 * It imports external base properties from a shared module and allows them to be overridden
 * by local test properties.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TestPropertySource(locations = {
    "file:../integrasjonspunkt/src/main/resources/config/application.properties",   // default base properties
    "classpath:application.properties"                                              // local test resource overrides
})
@ConfigurationPropertiesScan
public @interface UseFullTestConfiguration {
    String value() default "ImportsBasePropertiesAndOverrideWithLocalTestProperties";
}
