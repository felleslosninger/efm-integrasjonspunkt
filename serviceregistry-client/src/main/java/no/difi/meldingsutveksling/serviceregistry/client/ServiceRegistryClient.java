package no.difi.meldingsutveksling.serviceregistry.client;

import no.difi.meldingsutveksling.serviceregistry.common.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.common.ServiceRecord;
import retrofit2.Call;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class ServiceRegistryClient {

    public static final String DIFI = "991825827";

    public static void main(String[] args) throws IOException {
        IServiceRegistryClient service =
                ServiceGenerator.createService(IServiceRegistryClient.class);
        Call<List<ServiceRecord>> call = service.getServiceRecords(DIFI);
        List<ServiceRecord> serviceRecords = call.execute().body();

        Call<InfoRecord> callInfo = service.getCanReceive(DIFI);
        InfoRecord info = callInfo.execute().body();

        serviceRecords.get(0).getX509Certificate();
        System.out.println(serviceRecords);
        System.out.println(info);

    }

}
