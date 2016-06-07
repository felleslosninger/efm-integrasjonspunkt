package no.difi.meldingsutveksling.serviceregistry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRegistryLookupTest {

    static final String orgnumber = "12345678";

    @Mock
    private RestClient client;

    private Gson gson = new GsonBuilder().serializeNulls().create();
    private ServiceRegistryLookup service;

    @Before
    public void setup() {
        service = new ServiceRegistryLookup(client);
    }

    @Test
    public void organizationHasNoPrimary() {
        final HashMap<String, Object> srContent = new SRContentBuilder().withoutPrimaryServiceIdentifier().withServiceRecord(new ServiceRecord("identifier", orgnumber, "certificate", "payloadIdentifier", "http://localhost:6789")).build();

        when(client.getResource(orgnumber)).thenReturn(gson.toJson(srContent, Object.class));

        final ServiceRecord primaryServiceRecord = service.getPrimaryServiceRecord(orgnumber);

        assertEquals(primaryServiceRecord, ServiceRecord.EMPTY);
    }


    public static class SRContentBuilder {

        private String primaryServiceIdentifier;
        private List<HashMap<String, Object>> serviceRecords = new ArrayList<>();

        public SRContentBuilder withServiceRecord(ServiceRecord serviceRecord) {
            HashMap<String, Object> record = new HashMap<>();
            record.put("serviceRecord", serviceRecord);
            record.put("_links", "http://localhost/../" + serviceRecord.getPayloadIdentifier());
            this.serviceRecords.add(record);
            return this;
        }

        public SRContentBuilder withoutPrimaryServiceIdentifier() {
            this.primaryServiceIdentifier = null;
            return this;
        }

        public SRContentBuilder withPrimaryServiceIdentifier(String identifier) {
            this.primaryServiceIdentifier = identifier;
            return this;
        }

        public HashMap<String, Object> build() {
            InfoRecord infoRecord = new InfoRecord(primaryServiceIdentifier, orgnumber);

            final HashMap<String, Object> content = new HashMap<>();

            content.put("serviceRecords", this.serviceRecords);
            content.put("infoRecord", infoRecord);
            return content;
        }

    }
}