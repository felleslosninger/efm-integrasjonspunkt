package no.difi.meldingsutveksling.altinnv3.token;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service("SystemUserTokenProducer")
@RequiredArgsConstructor
public class SystemUserTokenProducer implements TokenProducer {

    public static final ZoneId DEFAULT_ZONE_ID = TimeZone.getTimeZone("Europe/Oslo").toZoneId();

    @Override
    public String produceToken(List<String> scopes) {
        try {
            return fetchMaskinportenToken(scopes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String fetchMaskinportenToken(List<String> scopes) throws Exception {

        // read the JWK private-public-key-bundle from test classpath
        RSAKey rsaJWK = RSAKey.parse(new String(this.getClass().getResourceAsStream("/311780735-sterk-ulydig-hund-da.jwk").readAllBytes()));

        JWSSigner signer = new RSASSASigner(rsaJWK);
        signer.supportedJWSAlgorithms().forEach(System.out::println);

        Map<String, Object> systemuserOrg = new HashMap<>();
        systemuserOrg.put("authority", "iso6523-actorid-upis");
        systemuserOrg.put("ID", "0192:311780735");

        Map<String, Object> authDetail = new HashMap<>();
        authDetail.put("systemuser_org", systemuserOrg);
        authDetail.put("type", "urn:altinn:systemuser");
        authDetail.put("externalRef", "311780735_integrasjonspunkt_systembruker_test");

        List<Map<String, Object>> authDetailsList = new ArrayList<>();
        authDetailsList.add(authDetail);


        // https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_jwtgrant
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .audience("https://test.maskinporten.no/")
            .issuer("b590f149-d0ba-4fca-b367-bccd9e444a00") // "eformidling-tenor-test-klient-02"
            .claim("scope", String.join(" ", scopes))
            .jwtID(UUID.randomUUID().toString())
            .claim("authorization_details", authDetailsList)
            .issueTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
            .expirationTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(120L)))
            .build();

        // maskinporten trenger kid eller x5c i header (kid for asymmetrisk nøkkel, x5c for virksomhetssertifikat)
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
            claims
        );

        // sign it with our private key rsa signer
        signedJWT.sign(signer);

        // just decode and debug print the info
        String serializedJwt = signedJWT.serialize();
        String[] chunks = serializedJwt.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        System.out.println("Payload: " + payload);
        System.out.println("SerializedJWT: " + serializedJwt);

        // https://docs.digdir.no/docs/Maskinporten/maskinporten_guide_apikonsument#registrere-klient-som-bruker-egen-nøkkel
        RestClient restClient = RestClient.create("https://test.maskinporten.no/token");
        String jsonTokenResponse = restClient.post()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body("grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + serializedJwt)
            .retrieve()
            .body(String.class);

        /*
        {
            "access_token":"eyJraWQiOiJiZFhMRVduRGpMSGpwRThPZnl5TUp4UlJLbVo3MUxCOHUxeUREbVBpdVQwIiwiYWxnIjoiUlMyNTYifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIucmVhZCIsImlzcyI6Imh0dHBzOi8vdGVzdC5tYXNraW5wb3J0ZW4ubm8vIiwiY2xpZW50X2FtciI6InByaXZhdGVfa2V5X2p3dCIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE3NTc0OTg0OTIsImlhdCI6MTc1NzQ5ODM3MiwiY2xpZW50X2lkIjoiODI2YWNiYmMtZWUxNy00OTQ2LWFmOTItY2Y0ODg1ZWJlOTUxIiwianRpIjoiaWkxWXZuUFBDVUhZbFp3OV9oVndyMVVoTFZRVWRpTlZfUUtOcnU0SkpHayIsImNvbnN1bWVyIjp7ImF1dGhvcml0eSI6ImlzbzY1MjMtYWN0b3JpZC11cGlzIiwiSUQiOiIwMTkyOjMxNDI0MDk3OSJ9fQ.X7-GEZh9IRBhDzyIqlF_IdOywLrnDCSyhYCd2bvVUAWL3wy7bY4joJN1U1kQr0tsgasTjuEv17JLzKzcLY1px8egseUeIFcHgUskKTICy_Twj9sHZntMkb2-mZvBfBGDMhb1kLMqj525u4Q8SVl70qdEP8KAIvWDl2yic7mQWF5pSMtXiAfHHo9HveG3WZQ47lhvvJyoCItgLY8yF5sdGF4yraxfrExGnp8LLFfpmPFQmGfsooXJuc9rU4yE7gFL8jztKX1jKImUXEndebCSaiOqUM3P0pna7S2iCMu76OeFC21kmZO0-VpBezPvbQqm9oTJWmUBqWWiV4tBvV9JQg-7t9cWygYJjw_nr7myL4BI2LATArQLl0CehKcGlwPq2_oCjXU_gl3Cf5PRpmUP1SV1ADHZJ0E1to9d_W6Ca6A5bXlCQLaQh13q5huaRmt36PqxmP8WTf1QEUNgcawSF8RhC_UyiGQ5rt663eQnLGRnTK535_6vIrx7k4xIws3I",
            "token_type":"Bearer",
            "expires_in":119,
            "scope":"altinn:broker.read"
        }
        */

        record Token(String access_token, String token_type, int expires_in, String scope) { }
        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var token = objectMapper.readValue(jsonTokenResponse, Token.class);

        return token.access_token;
    }

}
