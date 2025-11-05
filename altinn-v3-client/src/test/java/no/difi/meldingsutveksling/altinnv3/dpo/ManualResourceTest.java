package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.AltinnConfiguration;
import no.difi.meldingsutveksling.altinnv3.token.DpoTokenProducer;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import java.util.List;

@Disabled
@SpringBootTest(classes = {
    DpoTokenProducer.class,
    IntegrasjonspunktProperties.class,
    AltinnConfiguration.class,
})
@UseFullTestConfiguration
public class ManualResourceTest {

    @Inject
    DpoTokenProducer dpoTokenProducer;

    @Inject
    IntegrasjonspunktProperties properties;

    @Test
    public void getResource() {
        var accessToken = dpoTokenProducer.produceToken(
            properties.getDpo().getSystemUser(),
            List.of("altinn:broker.write","altinn:broker.read","altinn:serviceowner")
        );

        RestClient restClient = RestClient.create();
        var result = restClient.get()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/resource/meldingsutveksling_dpo")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .retrieve()
            .body(String.class)
            ;

        System.out.println(result);
    }

    @Test
    public void updateResource() {
        var accessToken = dpoTokenProducer.produceToken(properties.getDpo().getSystemUser(),
            List.of("altinn:broker.write","altinn:broker.read","altinn:serviceowner")
        );

        String body = """
            {
                "resourceId": "meldingsutveksling_dpo",
                "maxFileTransferSize": "1073741824",
                "fileTransferTimeToLive": "P30D",
                "PurgeFileTransferAfterAllRecipientsConfirmed" : true,
                "purgeFileTransferGracePeriod": "PT24H",
                "ExternalServiceCodeLegacy": "4192",
                "ExternalServiceEditionCodeLegacy": "270815",
                "UseManifestFileShim": true
            }
        """;

        RestClient restClient = RestClient.create();
        restClient.put()
            .uri("https://platform.tt02.altinn.no/broker/api/v1/resource/meldingsutveksling_dpo")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .body(body)
            .retrieve()
            .toBodilessEntity()
            ;
    }

}
