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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service("DpvTokenProducer")
@RequiredArgsConstructor
public class DpvTokenProducer implements TokenProducer {

    private final IntegrasjonspunktProperties properties;
    private final TokenService tokenService;
    private final TokenExchangeService tokenExchangeService;

    @Override
    @Cacheable(cacheNames = {"altinn.getDpvToken"})
    public String produceToken(List<String> scopes) {

        return tokenv2(scopes);

//        var config = new TokenConfig(properties.getDpv().getOidc(), properties.getDpv().getAltinnTokenExchangeUrl());
//        String token = tokenService.fetchToken(config, scopes, null);
//        return tokenExchangeService.exchangeToken(token, config.exchangeUrl());
    }

    public String tokenv2(List<String> scopes) {
        var config = new TokenConfig(properties.getDpv().getOidc(), properties.getDpo().getAltinnTokenExchangeUrl());

        String maskinportenToken = null;
        boolean useJwk = AuthenticationType.JWK.equals(properties.getDpv().getOidc().getAuthenticationType());
        if (useJwk) {
            maskinportenToken = fetchMaskinportenTokenUsingJwk(config, scopes, null);
        } else {
            maskinportenToken = tokenService.fetchToken(config, scopes, null);
        }

//        var altinnToken = tokenExchangeService.exchangeToken(maskinportenToken, config.exchangeUrl());
        return maskinportenToken;
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

        record Token(String access_token, String token_type, int expires_in, String scope) { }
        var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var token = objectMapper.readValue(jsonTokenResponse, Token.class);

        return token.access_token;
    }
}
