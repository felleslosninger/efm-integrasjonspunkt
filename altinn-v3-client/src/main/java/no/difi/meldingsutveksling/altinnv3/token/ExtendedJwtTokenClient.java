package no.difi.meldingsutveksling.altinnv3.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.oauth.JwtTokenConfig;
import no.difi.move.common.oauth.JwtTokenInput;
import no.difi.move.common.oauth.JwtTokenResponse;
import no.difi.move.common.oauth.OidcErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.cert.CertificateEncodingException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

public class ExtendedJwtTokenClient {

    public static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getTimeZone("Europe/Oslo");
    public static final ZoneId DEFAULT_ZONE_ID;
    private static final Logger log = LoggerFactory.getLogger(no.difi.move.common.oauth.JwtTokenClient.class);

    static {
        DEFAULT_ZONE_ID = DEFAULT_TIME_ZONE.toZoneId();
    }

    private final JwtTokenConfig config;
    private final WebClient wc;
    private final JWSHeader jwsHeader;
    private final RSASSASigner signer;

    public ExtendedJwtTokenClient(JwtTokenConfig config) {
        this.config = config;
        this.wc = WebClient.create(config.getTokenUri());
        KeystoreHelper keystoreHelper = new KeystoreHelper(config.getKeystore());
        this.jwsHeader = this.getJwsHeader(keystoreHelper);
        this.signer = this.getSigner(keystoreHelper);
    }

    private JWSHeader getJwsHeader(KeystoreHelper keystoreHelper) {
        return (new JWSHeader.Builder(JWSAlgorithm.RS256)).x509CertChain(this.getCertChain(keystoreHelper)).build();
    }

    private List<Base64> getCertChain(KeystoreHelper keystoreHelper) {
        try {
            return Collections.singletonList(Base64.encode(keystoreHelper.getX509Certificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("Could not get encoded certificate", e);
            throw new no.difi.move.common.oauth.JwtTokenClient.CertificateEncodingRuntimeException("Could not get encoded certificate", e);
        }
    }

    private RSASSASigner getSigner(KeystoreHelper keystoreHelper) {
        RSASSASigner s = new RSASSASigner(keystoreHelper.loadPrivateKey());
        if (keystoreHelper.shouldLockProvider()) {
            s.getJCAContext().setProvider(keystoreHelper.getKeyStore().getProvider());
        }
        return s;
    }

    @Retryable(
        value = {HttpClientErrorException.class},
        maxAttempts = Integer.MAX_VALUE,
        backoff = @Backoff(
            delay = 5000L,
            maxDelay = 3600000L,
            multiplier = (double) 3.0F
        )
    )
    public JwtTokenResponse fetchToken(JwtTokenInput input, AdditionalClaims additionalClaims) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new OidcErrorHandler());
        LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
        attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        attrMap.add("assertion", this.generateJWT(input, additionalClaims));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity(attrMap, headers);
        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);
        ResponseEntity<JwtTokenResponse> response = restTemplate.exchange(this.config.getTokenUri(), HttpMethod.POST, httpEntity, JwtTokenResponse.class, new Object[0]);
        return (JwtTokenResponse) response.getBody();
    }

    public String generateJWT(JwtTokenInput input, AdditionalClaims additionalClaims) {
        JWTClaimsSet.Builder builder = (new JWTClaimsSet.Builder())
            .audience((String) Optional.ofNullable(input.getAudience()).orElse(this.config.getAudience()))
            .issuer((String) Optional.ofNullable(input.getClientId()).orElse(this.config.getClientId()))
            .claim("scope", this.getScopeString(input)).jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
            .expirationTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(120L))
            );
        Optional.ofNullable(input.getConsumerOrg()).ifPresent((consumerOrg) -> builder.claim("consumer_org", consumerOrg));

        if (additionalClaims != null) {
            additionalClaims.getClaims().forEach(builder::claim);
        }


        // FIXME for JWK så må vi huske ekstra header med kid (fra jwk)
        JWTClaimsSet claims = builder.build();
        SignedJWT signedJWT = new SignedJWT(this.jwsHeader, claims);

        try {
            signedJWT.sign(this.signer);
        } catch (JOSEException e) {
            log.error("Error occured during signing of JWT", e);
        }

        String serializedJwt = signedJWT.serialize();
        String[] chunks = serializedJwt.split("\\.");
        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));
        log.info("Payload: {}", payload);
        log.debug("SerializedJWT: {}", serializedJwt);
        return serializedJwt;
    }

    private String getScopeString(JwtTokenInput input) {
        List<String> scopes = (List) Optional.ofNullable(input.getScopes()).orElse(this.config.getScopes());
        return scopes.stream().reduce((a, b) -> a + " " + b).orElse("");
    }

}
