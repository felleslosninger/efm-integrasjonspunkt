package no.difi.meldingsutveksling.serviceregistry.client;


import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

public interface IServiceRegistryClient {

    @GET("/organization/{orgnr}/services")
    Call<List<ServiceRecord>> getServiceRecords(
            @Path("orgnr") String orgNr);

    @GET("organization/{orgnr}/info")
    Call<InfoRecord> getCanReceive(
            @Path("orgnr") String orgNr);
}
