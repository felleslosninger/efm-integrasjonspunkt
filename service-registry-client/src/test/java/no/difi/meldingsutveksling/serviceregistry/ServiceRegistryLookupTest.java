package no.difi.meldingsutveksling.serviceregistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryLookupTest {

    private static final String ORGNR = "12345678";
    private static final String ORGNAME = "test";

    @Mock
    private RestClient client;


    private ServiceRegistryLookup service;
    private ServiceRecord post = new ServiceRecord("POST", "000", "certificate", "http://localhost:6789");
    private ServiceRecord edu = new ServiceRecord("EDU", "000", "certificate", "http://localhost:4567");

    @Before
    public void setup() {
        service = new ServiceRegistryLookup(client);
    }

    @Test
    public void organizationWithoutServiceRecord() {
        final String json = new SRContentBuilder().withoutPrimaryServiceIdentifier().withServiceRecord(post).withServiceRecord(edu).build();
        when(client.getResource("identifier/" + ORGNR)).thenReturn(json);

        final ServiceRecord primaryServiceRecord = this.service.getPrimaryServiceRecord(ORGNR);

        assertThat(primaryServiceRecord, is(ServiceRecord.EMPTY));
    }

    @Test
    public void organizationWithSingleServiceRecordHasPrimaryServiceRecord() {
        final String json = new SRContentBuilder().withoutPrimaryServiceIdentifier().withServiceRecord(edu).build();
        when(client.getResource("identifier/" + ORGNR)).thenReturn(json);

        final ServiceRecord primaryServiceRecord = service.getPrimaryServiceRecord(ORGNR);

        assertThat(primaryServiceRecord, is(edu));
    }

    @Test
    public void organizationWithTwoServiceRecordsHasNoPrimaryServiceRecord() {
        final String json = new SRContentBuilder().withoutPrimaryServiceIdentifier().withServiceRecord(post).withServiceRecord(edu).build();
        when(client.getResource("identifier/" + ORGNR)).thenReturn(json);

        final ServiceRecord primaryServiceRecord = service.getPrimaryServiceRecord(ORGNR);

        assertThat(primaryServiceRecord, is(ServiceRecord.EMPTY));
    }

    @Test
    public void organisationWithTwoServiceRecordsAndPrimaryOverride() {
        final String json = new SRContentBuilder().withPrimaryServiceIdentifier(edu.getServiceIdentifier()).withServiceRecord(post).withServiceRecord(edu).build();
        when(client.getResource("identifier/" + ORGNR)).thenReturn(json);

        final ServiceRecord serviceRecord = service.getPrimaryServiceRecord(ORGNR);

        assertThat(serviceRecord, is(edu));
    }

    public static class SRContentBuilder {
        private Gson gson = new GsonBuilder().serializeNulls().create();
        private String primaryServiceIdentifier;
        private List<HashMap<String, Object>> serviceRecords = new ArrayList<>();

        SRContentBuilder withServiceRecord(ServiceRecord serviceRecord) {
            HashMap<String, Object> record = new HashMap<>();
            record.put("serviceRecord", serviceRecord);
            record.put("_links", "http://localhost/../" + serviceRecord.getServiceIdentifier());
            this.serviceRecords.add(record);
            return this;
        }

        SRContentBuilder withoutPrimaryServiceIdentifier() {
            this.primaryServiceIdentifier = null;
            return this;
        }

        SRContentBuilder withPrimaryServiceIdentifier(String identifier) {
            this.primaryServiceIdentifier = identifier;
            return this;
        }

        String build() {
            EntityType entityType = new EntityType("Organisasjonsledd", "ORGL");
            InfoRecord infoRecord = new InfoRecord(primaryServiceIdentifier, ORGNR, ORGNAME, entityType);

            final HashMap<String, Object> content = new HashMap<>();

            content.put("serviceRecords", this.serviceRecords);
            content.put("infoRecord", infoRecord);
            return gson.toJson(content);
        }

    }
}