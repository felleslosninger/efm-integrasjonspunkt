package no.difi.meldingsutveksling.auth;

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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.*;

@Component
public class IdportenOidcTokenClient {

    private static final Logger log = LoggerFactory.getLogger(IdportenOidcTokenClient.class);

    private IntegrasjonspunktProperties props;

    @Autowired
    public IdportenOidcTokenClient(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    public IdportenOidcTokenResponse fetchToken(String scope) {
        RestTemplate restTemplate = new RestTemplate();

        LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
        attrMap.put("grant_type", Arrays.asList("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        attrMap.put("assertion", Arrays.asList(generateJWT(scope)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);

        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);

        URI oidcTokenUri;
        try {
             oidcTokenUri = props.getIdportenOidc().getBaseUrl().toURI();
        } catch (URISyntaxException e) {
            log.error("Error converting property to URI", e);
            throw new RuntimeException(e);
        }
        URI fullUri = UriComponentsBuilder.fromUri(oidcTokenUri)
                .pathSegment("idporten-oidc-provider/token")
                .build().toUri();

        ResponseEntity<IdportenOidcTokenResponse> response = restTemplate.exchange(fullUri, HttpMethod.POST,
                httpEntity, IdportenOidcTokenResponse.class);
        log.info("Response: {}", response.toString());

        return response.getBody();
    }

    private String generateJWT(String scope) {
        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel(props);

        List<Base64> certChain = new ArrayList<>();
        try {
            certChain.add(Base64.encode(nokkel.getX509Certificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("Could not get encoded certificate", e);
            throw new RuntimeException(e);
        }

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("https://eid-vag-opensso.difi.local/idporten-oidc-provider/")
                .issuer(props.getIdportenOidc().getIssuer())
                .claim("scope", scope)
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
