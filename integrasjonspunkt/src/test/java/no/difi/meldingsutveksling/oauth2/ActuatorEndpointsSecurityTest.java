package no.difi.meldingsutveksling.oauth2;

import no.difi.meldingsutveksling.clock.ClockConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.properties.ConfigurationPropertiesReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LogFileWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.mappings.MappingsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.AvailabilityProbesAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsEndpointAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.endpoint.web.WebMvcHealthEndpointExtensionAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.web.mappings.WebMvcMappingsAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies which actuator endpoints are exposed on /manage with the real management.* config from
 * config/application.properties (the "test" profile is deliberately not active, since it disables actuator),
 * both with and without difi.move.feature.enable-auth.
 * <p>
 * The endpoints that must not be exposed are auto-configured here on purpose, so a 404 proves that the
 * exposure whitelist filters them out, not that they are missing from the context.
 */
@Import(ClockConfig.class)
@WebMvcTest(Oauth2ClientSecurityConfig.class)
@ImportAutoConfiguration({
        ApplicationAvailabilityAutoConfiguration.class,
        EndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        JacksonEndpointAutoConfiguration.class,
        ManagementContextAutoConfiguration.class,
        HealthContributorAutoConfiguration.class,
        HealthContributorRegistryAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        WebMvcHealthEndpointExtensionAutoConfiguration.class,
        AvailabilityHealthContributorAutoConfiguration.class,
        AvailabilityProbesAutoConfiguration.class,
        DiskSpaceHealthContributorAutoConfiguration.class,
        InfoContributorAutoConfiguration.class,
        InfoEndpointAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class,
        PrometheusMetricsExportAutoConfiguration.class,
        MetricsEndpointAutoConfiguration.class,
        LoggersEndpointAutoConfiguration.class,
        LogFileWebEndpointAutoConfiguration.class,
        ConfigurationPropertiesReportEndpointAutoConfiguration.class,
        EnvironmentEndpointAutoConfiguration.class,
        HeapDumpWebEndpointAutoConfiguration.class,
        ThreadDumpEndpointAutoConfiguration.class,
        BeansEndpointAutoConfiguration.class,
        MappingsEndpointAutoConfiguration.class,
        WebMvcMappingsAutoConfiguration.class,
        ConditionsReportEndpointAutoConfiguration.class,
        ScheduledTasksEndpointAutoConfiguration.class
})
@TestPropertySource(properties = {
        "difi.security.user.name=testuser",
        "difi.security.user.password=testpassword"
})
class ActuatorEndpointsSecurityTest {

    @Nested
    @TestPropertySource(properties = "difi.move.feature.enable-auth=true")
    class WithAuth {

        @Autowired private MockMvc mvc;

        @ParameterizedTest
        @ValueSource(strings = {"/manage/health", "/manage/health/liveness", "/manage/health/readiness", "/manage/info"})
        void openEndpointsRespondWithoutCredentials(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isOk());
        }

        @Test
        void healthDetailsAreHiddenWithoutCredentials() throws Exception {
            mvc.perform(get("/manage/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        void healthDetailsAreShownWithCredentials() throws Exception {
            mvc.perform(get("/manage/health").with(httpBasic("testuser", "testpassword")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.diskSpace.details").exists());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/manage", "/manage/metrics", "/manage/prometheus", "/manage/loggers", "/manage/caches", "/manage/logfile"})
        void whitelistedEndpointsRequireCredentials(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/manage/metrics", "/manage/prometheus", "/manage/loggers"})
        void whitelistedEndpointsRespondWithCredentials(String path) throws Exception {
            mvc.perform(get(path).with(httpBasic("testuser", "testpassword"))).andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/manage/configprops", "/manage/env", "/manage/heapdump", "/manage/threaddump",
                "/manage/beans", "/manage/mappings", "/manage/conditions", "/manage/scheduledtasks"})
        void otherEndpointsAreNotExposed(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(httpBasic("testuser", "testpassword"))).andExpect(status().isNotFound());
        }
    }

    @Nested
    @TestPropertySource(properties = "difi.move.feature.enable-auth=false")
    class WithoutAuth {

        @Autowired private MockMvc mvc;

        @ParameterizedTest
        @ValueSource(strings = {"/manage/health", "/manage/health/liveness", "/manage/health/readiness", "/manage/info",
                "/manage/metrics", "/manage/prometheus", "/manage/loggers"})
        void whitelistedEndpointsRespond(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isOk());
        }

        @Test
        void healthDetailsAreHidden() throws Exception {
            mvc.perform(get("/manage/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/manage/configprops", "/manage/env", "/manage/heapdump", "/manage/threaddump",
                "/manage/beans", "/manage/mappings", "/manage/conditions", "/manage/scheduledtasks"})
        void otherEndpointsAreNotExposed(String path) throws Exception {
            mvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }
}
