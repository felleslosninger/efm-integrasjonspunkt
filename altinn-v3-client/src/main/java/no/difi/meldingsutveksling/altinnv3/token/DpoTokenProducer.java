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
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.AuthenticationType;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service("DpoTokenProducer")
@RequiredArgsConstructor
public class DpoTokenProducer implements TokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Override
    @Cacheable(cacheNames = {"altinn.getDpoToken"})
    public String produceToken(List<String> scopes) {

        var config = new TokenConfig(properties.getDpo().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());
        var authorizationClaims = new AuthorizationClaims(properties.getDpo().getAuthorizationDetails());

        String maskinportenToken = null;
        boolean useJwk = AuthenticationType.JWK.equals(properties.getDpo().getOidc().getAuthenticationType());
        if (useJwk) {
            maskinportenToken = fetchMaskinportenTokenUsingJwk(config, scopes, authorizationClaims);
        } else {
            maskinportenToken = tokenService.fetchToken(config, scopes, authorizationClaims);
        }

        // TODO : dette fungerer både med maskinporten token og altinn token, men levetiden er ulik
        // det kan være en fordel å benytte altinn token da dette har mye lenger levetid enn maskinporten token
        // maskinporten token har levetid på 120 sekunder (2 min)
        // altinn token har levetid på 1800 sekunder (30 min)

        var altinnToken = tokenExchangeService.exchangeToken(maskinportenToken, config.exchangeUrl());
        return altinnToken;

    }


    public static final ZoneId DEFAULT_ZONE_ID = TimeZone.getTimeZone("Europe/Oslo").toZoneId();

    @SneakyThrows
    private String fetchMaskinportenTokenUsingJwk(TokenConfig tokenConfig, List<String> scopes, AuthorizationClaims authorizationClaims) {

        // read the JWK private-public-key-bundle from the resource
        var jwkFile = tokenConfig.oidc().getJwk().getPath().getContentAsString(StandardCharsets.UTF_8);
        RSAKey rsaJWK = RSAKey.parse(jwkFile);

        JWSSigner signer = new RSASSASigner(rsaJWK);

        // https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_jwtgrant
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
            .audience(tokenConfig.oidc().getAudience())
            .issuer(tokenConfig.oidc().getClientId())
            .claim("scope", String.join(" ", scopes))
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
            .expirationTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(120L)))
            ;

        if (authorizationClaims != null) {
            authorizationClaims.getClaims().forEach(builder::claim);
        }

        // maskinporten trenger kid eller x5c i header (kid for asymmetrisk nøkkel, x5c for virksomhetssertifikat)
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
            builder.build()
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
        RestClient restClient = RestClient.create(tokenConfig.oidc().getUrl().toString());
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
