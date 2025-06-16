package altinn3;

import no.digdir.altinn3.rest.client.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.List;


@SpringBootTest
public class JustTesting {

    @Autowired
    ApiClient client;

    // https://docs.altinn.studio/api/correspondence/spec/#/Correspondence/post_correspondence_api_v1_correspondence

    @Test
    void callTheApi() throws Exception {

        RestClient restClient = client.getRestClient();

        var uri = client.getBasePath() + "/correspondence/api/v1/correspondence?role=RecipientAndSender&resourceId=eformidling-meldingsteneste-test";
        String accessToken = ApiUtils.retrieveAccessToken("altinn:correspondence.write altinn:correspondence.read altinn:serviceowner");

        var result = restClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(CorrespondenceList.class)
            ;

        result.ids().stream().map(it -> "Found ID = " + it).forEach(System.out::println);

    }

    // Record that maps : { "ids": [ "3fa85f64-5717-4562-b3fc-2c963f66afa6" ] }
    public record CorrespondenceList(List<String> ids) {};

}
