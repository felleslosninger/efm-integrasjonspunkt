package no.difi.meldingsutveksling.altinnv3.systemregister;

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
import no.difi.meldingsutveksling.altinnv3.token.TokenProducer;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service("SystemuserTokenProducer")
@RequiredArgsConstructor
public class SystemuserTokenProducer implements TokenProducer {

    public static final ZoneId DEFAULT_ZONE_ID = TimeZone.getTimeZone("Europe/Oslo").toZoneId();

    @Override
    @Cacheable(cacheNames = {"altinn.getSystemuserToken"})
    public String produceToken(List<String> scopes) {
        try {
            var maskinportenToken = fetchMaskinportenToken(scopes);
//            Ikke nødvendig med Altinn tokens egentlig, fungerer med vanlig MP token
//            System.out.println("MaskinportenToken: " + maskinportenToken);
//            var altinnToken = exchangeAltinnToken(maskinportenToken);
//            System.out.println("AltinnToken: " + altinnToken);
            return maskinportenToken;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String fetchMaskinportenToken(List<String> scopes) throws Exception {

        // read the JWK private-public-key-bundle from test classpath
        RSAKey rsaJWK = RSAKey.parse(new String(this.getClass().getResourceAsStream("/311780735-sterk-ulydig-hund-da.jwk").readAllBytes()));

        JWSSigner signer = new RSASSASigner(rsaJWK);
        signer.supportedJWSAlgorithms().forEach(System.out::println);

        // https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_jwtgrant
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .audience("https://test.maskinporten.no/")
            .issuer("b590f149-d0ba-4fca-b367-bccd9e444a00") // "eformidling-tenor-test-klient-02"
            .claim("scope", String.join(" ", scopes))
            .jwtID(UUID.randomUUID().toString())
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

        record Token(String access_token, String token_type, int expires_in, String scope) { }
        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var token = objectMapper.readValue(jsonTokenResponse, Token.class);

        return token.access_token;
    }

    private String exchangeAltinnToken(String maskinportenToken) throws Exception {
        RestClient restClient = RestClient.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten");
        String token = restClient.get()
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
        return token;
    }

}
