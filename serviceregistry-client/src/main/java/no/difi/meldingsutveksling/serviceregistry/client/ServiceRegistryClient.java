package no.difi.meldingsutveksling.serviceregistry.client;


import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecords;
import retrofit2.Call;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ServiceRegistryClient {

    public static final String DIFI = "910077473";

    public static void main(String[] args) throws IOException {
        IServiceRegistryClient service =
                ServiceGenerator.createService(IServiceRegistryClient.class);
        Call<ServiceRecords> call = service.getServiceRecords(DIFI);
        List<ServiceRecord> serviceRecords = call.execute().body().getServiceRecords();


        System.out.println(serviceRecords.get(0).getX509Certificate());
        System.out.println(serviceRecords);

    }

}
