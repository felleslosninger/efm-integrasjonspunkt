package no.difi.meldingsutveksling.auth;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class OidcTokenClient {

    private static final String CLIENT_ID_PREFIX = "MOVE_IP_";
    private static final Logger log = LoggerFactory.getLogger(OidcTokenClient.class);

    private IntegrasjonspunktProperties props;

    @Autowired
    public OidcTokenClient(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    public IdportenOidcTokenResponse fetchToken() {
        RestTemplate restTemplate = new RestTemplate();

        LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
        attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        attrMap.add("assertion", generateJWT());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);

        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);

        URI accessTokenUri;
        try {
             accessTokenUri = props.getOidc().getUrl().toURI();
        } catch (URISyntaxException e) {
            log.error("Error converting property to URI", e);
            throw new RuntimeException(e);
        }

        ResponseEntity<IdportenOidcTokenResponse> response = restTemplate.exchange(accessTokenUri, HttpMethod.POST,
                httpEntity, IdportenOidcTokenResponse.class);
        log.info("Response: {}", response.toString());

        return response.getBody();
    }

    public String generateJWT() {
        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel(props.getOidc().getKeystore());

        List<Base64> certChain = new ArrayList<>();
        try {
            certChain.add(Base64.encode(nokkel.getX509Certificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("Could not get encoded certificate", e);
            throw new RuntimeException(e);
        }

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();
        String scopes = "";
        if (props.getOidc().getScopes() != null) {
            scopes = props.getOidc().getScopes().stream().reduce((a, b) -> a + " " + b).orElse("");
        }

        String clientId = props.getOidc().getClientId();
        if (Strings.isNullOrEmpty(clientId)) {
            clientId = CLIENT_ID_PREFIX+props.getOrg().getNumber();
        }
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(props.getOidc().getAudience())
                .issuer(clientId)
                .claim("scope", scopes)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(120)))
                .build();

        JWSSigner signer = new RSASSASigner(nokkel.loadPrivateKey());
        SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            log.error("Error occured during signing of JWT", e);
        }

        String serializedJwt = signedJWT.serialize();
        log.info("SerializedJWT: {}", serializedJwt);

        return serializedJwt;
    }

}
