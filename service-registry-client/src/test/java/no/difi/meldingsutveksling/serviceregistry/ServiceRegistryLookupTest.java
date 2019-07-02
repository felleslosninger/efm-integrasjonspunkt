package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ServiceRegistryLookupTest.Config.class)
public class ServiceRegistryLookupTest {

    private static final String ORGNR = "12345678";
    private static final String ORGNAME = "test";
    private static final String DEFAULT_PROCESS = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";
    private static final String DEFAULT_DOCTYPE = "urn:no:difi:arkivmelding:xsd::arkivmelding";

    @Configuration
    @EnableCaching
    static class Config {

        // Simulating your caching configuration
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(ServiceRegistryLookup.CACHE_GET_SAS_KEY);
        }
    }

    @Autowired
    private CacheManager cacheManager;

    private RestClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ServiceRegistryLookup service;
    private ServiceRecord dpo = new ServiceRecord(DPO, "000", "certificate", "http://localhost:4567");
    private String query;

    @Before
    public void setup() {
        client = mock(RestClient.class);
        final IntegrasjonspunktProperties properties = mock(IntegrasjonspunktProperties.class);
        SasKeyRepository sasKeyRepoMock = mock(SasKeyRepository.class);
        IntegrasjonspunktProperties.Arkivmelding arkivmeldingProps = new IntegrasjonspunktProperties.Arkivmelding().setDefaultProcess("foo");
        when(properties.getArkivmelding()).thenReturn(arkivmeldingProps);
        IntegrasjonspunktProperties.Arkivmelding arkivmelding = mock(IntegrasjonspunktProperties.Arkivmelding.class);
        when(arkivmelding.getDefaultProcess()).thenReturn(DEFAULT_PROCESS);
        when(properties.getArkivmelding()).thenReturn(arkivmelding);
        service = new ServiceRegistryLookup(client, properties, sasKeyRepoMock, new ObjectMapper());
        query = null;
        dpo.setProcess(DEFAULT_PROCESS);
        dpo.setDocumentTypes(Collections.singletonList(DEFAULT_DOCTYPE));
    }

    @Test(expected = ServiceRegistryLookupException.class)
    public void organizationWithoutServiceRecord() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().build();
        when(client.getResource("identifier/" + ORGNR, query)).thenReturn(json);

        this.service.getServiceRecord(ORGNR);
    }

    @Test(expected = ServiceRegistryLookupException.class)
    public void noEntityForOrganization() throws BadJWSException, ServiceRegistryLookupException {
        when(client.getResource("identifier/" + ORGNR, query)).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        this.service.getServiceRecord(ORGNR);
    }

    @Test(expected = ServiceRegistryLookupException.class)
    public void organizationWithoutServiceRecords() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().build();
        when(client.getResource("identifier/" + ORGNR, query)).thenReturn(json);

        this.service.getServiceRecord(ORGNR, DPO);
    }

    @Test
    public void organizationWithSingleServiceRecordHasServiceRecord() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().withServiceRecord(dpo).build();
        when(client.getResource("identifier/" + ORGNR, query)).thenReturn(json);

        final ServiceRecord serviceRecord = service.getServiceRecord(ORGNR);

        assertThat(serviceRecord, is(dpo));
    }

    @Test
    public void organizationWithSingleServiceRecordHasServiceRecords() throws BadJWSException, ServiceRegistryLookupException {
        final String json = new SRContentBuilder().withServiceRecord(dpo).build();
        when(client.getResource("identifier/" + ORGNR, query)).thenReturn(json);

        ServiceRecord serviceRecord = service.getServiceRecord(ORGNR, DPO);

        assertThat(serviceRecord, is(dpo));
    }

    @Test
    public void testSasKeyCacheInvalidation() throws BadJWSException {
        when(client.getResource("sastoken")).thenReturn("123").thenReturn("456");

        assertThat(service.getSasKey(), is("123"));
        cacheManager.getCache(ServiceRegistryLookup.CACHE_GET_SAS_KEY).clear();
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
                return new ObjectMapper().writeValueAsString(resource);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}