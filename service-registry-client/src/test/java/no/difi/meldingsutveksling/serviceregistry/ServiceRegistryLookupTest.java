package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.CacheConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.jackson.PartnerIdentifierModule;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ServiceRegistryLookupTest.Config.class)
public class ServiceRegistryLookupTest {

    private static final Iso6523 ORGNR = Iso6523.of(ICD.NO_ORG, "12345678");
    private static final Iso6523 ORGNR2 = Iso6523.of(ICD.NO_ORG, "23456789");
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
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new PartnerIdentifierModule());
            return objectMapper;
        }
    }

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ServiceRegistryLookup service;

    @MockBean
    private IntegrasjonspunktProperties properties;

    @MockBean
    private SasKeyRepository sasKeyRepoMock;

    @MockBean
    private RestClient client;

    private ServiceRecord dpo = new ServiceRecord(DPO, ORGNR, "certificate", "http://localhost:4567");

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
        cacheManager.getCacheNames().forEach(p -> {
            Objects.requireNonNull(cacheManager.getCache(p)).clear();
        });
    }

    @SneakyThrows
    @Test
    public void organizationWithoutServiceRecord() {
        final String json = new SRContentBuilder().build();
        when(client.getResource(eq("identifier/{identifier}"), anyMap())).thenReturn(json);

        assertThrows(ServiceRegistryLookupException.class, () -> this.service.getServiceRecord(SRParameter.builder(ORGNR).build()));
    }

    @Test
    public void noEntityForOrganization() throws BadJWSException, ServiceRegistryLookupException {
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
        cacheManager.getCache(CacheConfig.CACHE_GET_SAS_KEY).clear();
        assertThat(service.getSasKey(), is("456"));
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
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new PartnerIdentifierModule());
                return objectMapper.writeValueAsString(resource);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}