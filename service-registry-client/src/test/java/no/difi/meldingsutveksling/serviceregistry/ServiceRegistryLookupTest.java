package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.client.ServiceRegistryRestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(classes = ServiceRegistryLookupTest.Config.class)
public class ServiceRegistryLookupTest {

    private static final String ORGNR = "12345678";
    private static final String ORGNR2 = "23456789";
    private static final String ORGNAME = "test";
    private static final String DEFAULT_PROCESS = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static final String DEFAULT_DOCTYPE = "urn:no:difi:arkivmelding:xsd::arkivmelding";

    @Configuration
    @EnableCaching
    @Import({ServiceRegistryClient.class, ServiceRegistryLookup.class})
    static class Config {

        // Simulating your caching configuration
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ServiceRegistryLookup service;

    @MockitoBean
    private IntegrasjonspunktProperties properties;

    @MockitoBean
    private SasKeyRepository sasKeyRepoMock;

    @MockitoBean
    private ServiceRegistryRestClient client;

    private final ServiceRecord dpo = new ServiceRecord(DPO, "000", "certificate", "http://localhost:4567");

    @BeforeEach
    public void setup() {
        IntegrasjonspunktProperties.Arkivmelding arkivmeldingProps = new IntegrasjonspunktProperties.Arkivmelding().setDefaultProcess("foo");
        when(properties.getArkivmelding()).thenReturn(arkivmeldingProps);
        IntegrasjonspunktProperties.Arkivmelding arkivmelding = mock(IntegrasjonspunktProperties.Arkivmelding.class);
        IntegrasjonspunktProperties.FeatureToggle feature = mock(IntegrasjonspunktProperties.FeatureToggle.class);
        when(arkivmelding.getDefaultProcess()).thenReturn(DEFAULT_PROCESS);
        when(properties.getArkivmelding()).thenReturn(arkivmelding);
        when(properties.getFeature()).thenReturn(feature);
        when(properties.getFeature().isEnableDsfPrintLookup()).thenReturn(true);
        dpo.setProcess(DEFAULT_PROCESS);
        dpo.setDocumentTypes(Collections.singletonList(DEFAULT_DOCTYPE));
    }

    @AfterEach
    public void after() {
        cacheManager.getCacheNames().forEach(p -> Objects.requireNonNull(cacheManager.getCache(p)).clear());
    }

    @SneakyThrows
    @Test
    public void organizationWithoutServiceRecord() {
        final String json = new SRContentBuilder().build();
        when(client.getResource(eq("identifier/{identifier}"), anyMap())).thenReturn(json);

        assertThrows(ServiceRegistryLookupException.class, () -> this.service.getServiceRecord(SRParameter.builder(ORGNR).build()));
    }

    @Test
    public void noEntityForOrganization() throws BadJWSException {
        when(client.getResource(eq("identifier/{identifier}"), anyMap())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(ServiceRegistryLookupException.class, () -> this.service.getServiceRecord(SRParameter.builder(ORGNR).build()));
    }

    @Test
    public void organizationWithSingleServiceRecordHasServiceRecord() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().withServiceRecord(dpo).build();
        when(client.getResource(eq("identifier/{identifier}"), anyMap())).thenReturn(json);

        final ServiceRecord serviceRecord = service.getServiceRecord(SRParameter.builder(ORGNR).build());

        assertThat(serviceRecord, is(dpo));
    }

    @Test
    public void testThatConversationIdIsNotInCacheKey() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().withServiceRecord(dpo).build();
        when(client.getResource(any(), anyMap())).thenReturn(json);

        String conversationId1 = UUID.randomUUID().toString();
        String conversationId2 = UUID.randomUUID().toString();
        service.getServiceRecord(SRParameter.builder(ORGNR).conversationId(conversationId1).build());
        service.getServiceRecord(SRParameter.builder(ORGNR).conversationId(conversationId1).build());
        service.getServiceRecord(SRParameter.builder(ORGNR).conversationId(conversationId2).build());
        service.getServiceRecord(SRParameter.builder(ORGNR).build());
        service.getServiceRecord(SRParameter.builder(ORGNR2).build());

        verify(client, times(2)).getResource(anyString(), anyMap());
    }

    @Test
    public void testSasKeyCacheInvalidation() throws BadJWSException {
        when(client.getResource(eq("sastoken"))).thenReturn("123").thenReturn("456");

        assertThat(service.getSasKey(), is("123"));
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.CACHE_GET_SAS_KEY)).clear();
        assertThat(service.getSasKey(), is("456"));
    }

    @Test
    public void getInfoRecordWhenUnknownServiceIdentifierFromServiceRegistry() throws BadJWSException {
        when(client.getResource(any(), anyMap())).thenReturn("""
            {
              "infoRecord": {
                "identifier": "910077473",
                "organizationName": "TEST - C4",
                "postadresse": null,
                "entityType": {
                  "name": "ORGL"
                }
              },
              "serviceRecords": [
                {
                  "organisationNumber": "910077473",
                  "pemCertificate": "-----BEGIN CERTIFICATE-----\\nMIICujCCAaKgAwIBAgIEXIe4TjANBgkqhkiG9w0BAQsFADAeMRwwGgYDVQQDDBNE\\nSUZJIHRlc3QgOTEwMDc1OTE4MCAXDTE5MDMxMjEzNDY1NFoYDzIxMTkwMzEyMTM0\\nNjU0WjAeMRwwGgYDVQQDDBNESUZJIHRlc3QgOTEwMDc1OTE4MIIBIjANBgkqhkiG\\n9w0BAQEFAAOCAQ8AMIIBCgKCAQEArdbwXXtDgA3SJiNlYoG1F65zzOMqxJyd4Rvl\\n8ofMP7gVfze9E2ydRg05m/dzQPIRhOPPlzsYwBBtkIH+iy+lJ6lh+l62SLXLhUCF\\n4Z36uxbIIw8C/w0VMuiuoYwMig7AKX+hwqa2qCmL45b9eRMXkMMrZuWQvloXCONQ\\nyCrQ5uNkZ/sGCiHqPekobjQ4AU0m/W0O2+NbyBsddZQ88BnBhEZyMj7K8xul0pM0\\nT5JkGybfKVBYooyHFeWfJTZ+z8sae8cB4b6XJtjil3MPfOgIU1W2cj8hkY7DyGfI\\n7pjwAKNL45S2F0v2jaI37a5p4x5BzSvmDksh2pmevkwGBHRkMQIDAQABMA0GCSqG\\nSIb3DQEBCwUAA4IBAQAKZACSKEWNvcVzKuP/e17w/abzLRB4iIrFktb1wlJV4Zab\\n5LP8spP6yfpTRSnle7P+K145dSSYCnsutFe7aZ0wOSLKQLOUWCmZFHSXlSYymss1\\nx3aRk8Cg3itMuRwrViugHpWpJq+TRMq863W7sPgGLLAoGBIdWa9swI9JdazGD7/o\\nyUTK1+GOI2yciDQaFiH+HlP9auGCs8X0HlZizYtJqivbSyGM9nH0Z0/T5asTHFig\\nARLdWrnH1oKfEfE3sN/whPXoZtHyD/39u+Sk/FIzjnNEIrSHgpkSN6lY3DfjYH+k\\nF/OZ/A6+cmGBmH7aMO7GIVR2NZgUeVj7Bu1W7WkP\\n-----END CERTIFICATE-----",
                  "process": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                  "documentTypes": [
                    "urn:no:difi:arkivmelding:xsd::arkivmelding"
                  ],
                  "service": {
                    "identifier": "STRANGE",
                    "endpointUrl": "http://localhost:9800",
                    "serviceCode": "4192",
                    "serviceEditionCode": "270815"
                  }
                }
              ]
            }
            """);

        assertNotNull(service.getInfoRecord(ORGNR));
    }


    public static class SRContentBuilder {
        private ServiceRecord serviceRecord;

        SRContentBuilder withServiceRecord(ServiceRecord serviceRecord) {
            this.serviceRecord = serviceRecord;
            return this;
        }

        String build() {
            EntityType entityType = new EntityType("Organisasjonsledd", "ORGL");
            InfoRecord infoRecord = new InfoRecord(ORGNR, ORGNAME, entityType);

            IdentifierResource resource = new IdentifierResource()
                .setInfoRecord(infoRecord)
                .setServiceRecords(serviceRecord == null ? Collections.emptyList() : Collections.singletonList(this.serviceRecord));

            try {
                return new ObjectMapper().writeValueAsString(resource);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
