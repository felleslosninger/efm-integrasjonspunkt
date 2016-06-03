package no.difi.meldingsutveksling.serviceregistry.client;


import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecords;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface IServiceRegistryClient {

    @GET("/organization/{orgnr}/")
    Call<ServiceRecords> getServiceRecords(
            @Path("orgnr") String orgNr);

}
