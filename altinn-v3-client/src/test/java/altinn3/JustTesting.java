package altinn3;

import no.digdir.altinn3.rest.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.List;


@SpringBootTest
public class JustTesting {



    // https://docs.altinn.studio/api/correspondence/spec/#/Correspondence/post_correspondence_api_v1_correspondence

    @Test
    void createNewCorrespondence() throws Exception {

        InitializeCorrespondencesExt request = new InitializeCorrespondencesExt();
        BaseCorrespondenceExt correspondence = new BaseCorrespondenceExt();
        correspondence.setResourceId("eformidling-meldingsteneste-test");
        correspondence.setSender("0192:991825827");
        correspondence.setSendersReference("string");
        correspondence.setIsConfirmationNeeded(false);
        InitializeCorrespondenceContentExt content = new InitializeCorrespondenceContentExt();
        content.setLanguage("nb");
        content.setMessageTitle("Testmelding fra Digdir");
        content.setMessageBody("Testmelding fra Digdir");
        content.setMessageSummary("Testmelding fra Digdir");
        correspondence.setContent(content);
        request.setCorrespondence(correspondence);
        request.setRecipients(List.of("urn:altinn:organization:identifier-no:310654302"));

        // organization(urn:altinn:organization:identifier-no:ORGNR)
        // national identity number(urn:altinn:person:identifier-no:SSN)

        var uri = "https://platform.tt02.altinn.no/correspondence/api/v1/correspondence";
        String accessToken = ApiUtils.retrieveAccessToken("altinn:correspondence.write altinn:correspondence.read altinn:serviceowner");

        RestClient restClient = RestClient.create();
        var result = restClient.post()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .body(request)
            .retrieve()
            .body(InitializeCorrespondencesResponseExt.class)
            ;

        result.getCorrespondences().stream()
            .forEach(c -> System.out.println(c.getCorrespondenceId()));

    }



        @Test
    void callUsingGeneratedModel() throws Exception {

        RestClient restClient = RestClient.create();

        var uri = "https://platform.tt02.altinn.no/correspondence/api/v1/correspondence?role=RecipientAndSender&resourceId=eformidling-meldingsteneste-test";
        String accessToken = ApiUtils.retrieveAccessToken("altinn:correspondence.write altinn:correspondence.read altinn:serviceowner");

        var result = restClient.get()
            .uri(uri)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(CorrespondencesExt.class)
            ;

        System.out.println(result);
    // 01977ced-6722-7e1e-a468-a31e70551639
    }

    @Test
    void callTheApiManually() throws Exception {

        RestClient restClient = RestClient.create();

        var uri = "https://platform.tt02.altinn.no/correspondence/api/v1/correspondence?role=RecipientAndSender&resourceId=eformidling-meldingsteneste-test";
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
