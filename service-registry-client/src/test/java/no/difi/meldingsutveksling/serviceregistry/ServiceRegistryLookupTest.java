package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.Notification;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryLookupTest {

    private static final String ORGNR = "12345678";
    private static final String ORGNAME = "test";
    private static final String DEFAULT_PROCESS = "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0";

    @Mock
    private RestClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ServiceRegistryLookup service;
    private ServiceRecord dpo = new ServiceRecord(DPO, "000", "certificate", "http://localhost:4567");
    private String query;

    @Before
    public void setup() {
        final IntegrasjonspunktProperties properties = mock(IntegrasjonspunktProperties.class);
        SasKeyRepository sasKeyRepoMock = mock(SasKeyRepository.class);
        IntegrasjonspunktProperties.Arkivmelding arkivmeldingProps = new IntegrasjonspunktProperties.Arkivmelding().setDefaultProcess("foo");
        when(properties.getArkivmelding()).thenReturn(arkivmeldingProps);
        IntegrasjonspunktProperties.Arkivmelding arkivmelding = mock(IntegrasjonspunktProperties.Arkivmelding.class);
        when(arkivmelding.getDefaultProcess()).thenReturn(DEFAULT_PROCESS);
        when(properties.getArkivmelding()).thenReturn(arkivmelding);
        service = new ServiceRegistryLookup(client, properties, sasKeyRepoMock, new ObjectMapper());
        query = Notification.NOT_OBLIGATED.createQuery();
        dpo.setProcess(DEFAULT_PROCESS);
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
    public void testSasKeyCacheInvalidation() throws BadJWSException, ServiceRegistryLookupException {
        when(client.getResource("sastoken")).thenReturn("123").thenReturn("456");

        assertThat(service.getSasKey(), is("123"));
        service.invalidateSasKey();
        assertThat(service.getSasKey(), is("456"));
    }

    public static class SRContentBuilder {
        private Gson gson = new GsonBuilder().serializeNulls().create();
        private ServiceRecord serviceRecord;

        SRContentBuilder withServiceRecord(ServiceRecord serviceRecord) {
            this.serviceRecord = serviceRecord;
            return this;
        }

        String build() {
            EntityType entityType = new EntityType("Organisasjonsledd", "ORGL");
            InfoRecord infoRecord = new InfoRecord(ORGNR, ORGNAME, entityType);

            final HashMap<String, Object> content = new HashMap<>();

            if (this.serviceRecord == null) {
                content.put("serviceRecord", ServiceRecord.EMPTY);
                content.put("serviceRecords", Lists.newArrayList());
            } else {
                content.put("serviceRecord", this.serviceRecord);
                content.put("serviceRecords", Lists.newArrayList(this.serviceRecord));
            }
            content.put("infoRecord", infoRecord);
            content.put("failedServiceIdentifiers", Lists.newArrayList());
            content.put("securitylevels", Maps.newHashMap());
            return gson.toJson(content);
        }

    }
}