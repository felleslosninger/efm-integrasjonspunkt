package no.difi.meldingsutveksling.altinnv3;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class TestMaskinportenTokenUsingJwk {

    public static final ZoneId DEFAULT_ZONE_ID = TimeZone.getTimeZone("Europe/Oslo").toZoneId();

    @Disabled
    @Test
    public void getMaskinportenToken() throws Exception{

        var bearer = fetchMaskinportenToken();
        System.out.println("Bearer: " + bearer);

    }

    @Disabled
    @Test
    public void exchangeToAltinnToken() throws Exception {

        var maskinportenToken = fetchMaskinportenToken();
        System.out.println("MaskinportenToken: " + maskinportenToken);
        /*
        MaskinportenToken: eyJraWQiOiJiZFhMRVduRGpMSGpwRThPZnl5TUp4UlJLbVo3MUxCOHUxeUREbVBpdVQwIiwiYWxnIjoiUlMyNTYifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIucmVhZCIsImlzcyI6Imh0dHBzOi8vdGVzdC5tYXNraW5wb3J0ZW4ubm8vIiwiY2xpZW50X2FtciI6InByaXZhdGVfa2V5X2p3dCIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE3NTc0OTg0OTIsImlhdCI6MTc1NzQ5ODM3MiwiY2xpZW50X2lkIjoiODI2YWNiYmMtZWUxNy00OTQ2LWFmOTItY2Y0ODg1ZWJlOTUxIiwianRpIjoiaWkxWXZuUFBDVUhZbFp3OV9oVndyMVVoTFZRVWRpTlZfUUtOcnU0SkpHayIsImNvbnN1bWVyIjp7ImF1dGhvcml0eSI6ImlzbzY1MjMtYWN0b3JpZC11cGlzIiwiSUQiOiIwMTkyOjMxNDI0MDk3OSJ9fQ.X7-GEZh9IRBhDzyIqlF_IdOywLrnDCSyhYCd2bvVUAWL3wy7bY4joJN1U1kQr0tsgasTjuEv17JLzKzcLY1px8egseUeIFcHgUskKTICy_Twj9sHZntMkb2-mZvBfBGDMhb1kLMqj525u4Q8SVl70qdEP8KAIvWDl2yic7mQWF5pSMtXiAfHHo9HveG3WZQ47lhvvJyoCItgLY8yF5sdGF4yraxfrExGnp8LLFfpmPFQmGfsooXJuc9rU4yE7gFL8jztKX1jKImUXEndebCSaiOqUM3P0pna7S2iCMu76OeFC21kmZO0-VpBezPvbQqm9oTJWmUBqWWiV4tBvV9JQg-7t9cWygYJjw_nr7myL4BI2LATArQLl0CehKcGlwPq2_oCjXU_gl3Cf5PRpmUP1SV1ADHZJ0E1to9d_W6Ca6A5bXlCQLaQh13q5huaRmt36PqxmP8WTf1QEUNgcawSF8RhC_UyiGQ5rt663eQnLGRnTK535_6vIrx7k4xIws3I

        {
            "scope": "altinn:broker.read",
            "iss": "https://test.maskinporten.no/",
            "client_amr": "private_key_jwt",
            "token_type": "Bearer",
            "exp": 1757498492,
            "iat": 1757498372,
            "client_id": "826acbbc-ee17-4946-af92-cf4885ebe951",
            "jti": "ii1YvnPPCUHYlZw9_hVwr1UhLVQUdiNV_QKNru4JJGk",
            "consumer": {
                "authority": "iso6523-actorid-upis",
                "ID": "0192:314240979"
            }
        }
        */

        var altinnToken = exchangeAltinnToken(maskinportenToken);
        System.out.println("AltinnToken: " + altinnToken);
        /*
        AltinnToken: eyJhbGciOiJSUzI1NiIsImtpZCI6IjcxOUFGOTRFNDQ1MzE0Q0RDMjk1Rjk1MjUzODU4MDU0RjhCQ0FDODYiLCJ4NXQiOiJjWnI1VGtSVEZNM0NsZmxTVTRXQVZQaThySVkiLCJ0eXAiOiJKV1QifQ.eyJzY29wZSI6ImFsdGlubjpicm9rZXIucmVhZCIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJleHAiOjE3NTc1MDAxNzIsImlhdCI6MTc1NzQ5ODM3MiwiY2xpZW50X2lkIjoiODI2YWNiYmMtZWUxNy00OTQ2LWFmOTItY2Y0ODg1ZWJlOTUxIiwiY29uc3VtZXIiOnsiYXV0aG9yaXR5IjoiaXNvNjUyMy1hY3RvcmlkLXVwaXMiLCJJRCI6IjAxOTI6MzE0MjQwOTc5In0sInVybjphbHRpbm46b3JnTnVtYmVyIjoiMzE0MjQwOTc5IiwidXJuOmFsdGlubjphdXRoZW50aWNhdGVtZXRob2QiOiJtYXNraW5wb3J0ZW4iLCJ1cm46YWx0aW5uOmF1dGhsZXZlbCI6MywiaXNzIjoiaHR0cHM6Ly9wbGF0Zm9ybS50dDAyLmFsdGlubi5uby9hdXRoZW50aWNhdGlvbi9hcGkvdjEvb3BlbmlkLyIsImp0aSI6IjAyN2RhZDEzLTFhZWItNDdmOC1hYjhlLTRiMGIyZjY0MWVjZSIsIm5iZiI6MTc1NzQ5ODM3Mn0.kvfV72Tm8UFMSqej1Z3_wZanBNSmUk0lkD4XeqTPFY68ZQpNtlUmLiZeJDs37uVV45wl4NwhyqgGukKZls1Nja9u342Rb06fSGA4wFnXQmRMJQK7A-heyB--VuF2QYNRB2mPvGUzwSd0G-xGIcQSbOIeLhJroXBITypD0Zs9EexvTzu2jNpf8Aa_mT5tx7Z-5wjqtr1fUdc2-TkMfzSAnSXxZCf0yrds933BG_OwSWyXgIiL_hICP1qv4ARDtn5STGzX2Vhp5zqLL0oU2QZTBGsWtq4UkulbFEmkixmmMRQZnqMGnOpzh-D-1--YmVYVIlcC3xeIFHe3CucmzqIMqA

        {
          "scope": "altinn:broker.read",
          "token_type": "Bearer",
          "exp": 1757500172,
          "iat": 1757498372,
          "client_id": "826acbbc-ee17-4946-af92-cf4885ebe951",
          "consumer": {
            "authority": "iso6523-actorid-upis",
            "ID": "0192:314240979"
          },
          "urn:altinn:orgNumber": "314240979",
          "urn:altinn:authenticatemethod": "maskinporten",
          "urn:altinn:authlevel": 3,
          "iss": "https://platform.tt02.altinn.no/authentication/api/v1/openid/",
          "jti": "027dad13-1aeb-47f8-ab8e-4b0b2f641ece",
          "nbf": 1757498372
        }
        */

    }

    @Disabled
    @Test
    public void createRsaKeys() throws Exception {

        // generer RSA-nøkkel (2048-bit)
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
            .keyID("my-key-id")     // ‘kid’-verdien må være unik blant alle Maskinportens kunder (!!!)
            .keyUse(KeyUse.SIGNATURE)   // mulig denne ikke trengs ?
            .algorithm(JWSAlgorithm.RS256)
            .generate();

        // privat + offentlig nøkkel (bruk direkte i testen ovenfor)
        System.out.println("Privat JWK: " + rsaJWK.toJSONString());

        // bare offentlig del (distribueres til klienter/verifisering, legges inn i maskinporten)
        System.out.println("Offentlig JWK: " + rsaJWK.toPublicJWK().toJSONString());

    }

    private String fetchMaskinportenToken() throws Exception {

        RSAKey rsaJWK = RSAKey.parse("""
            {
                "p": "4oCSHbRdc4PmW-sqm0VoxXTt6OmCYF3mOQw-hddK35yAGdm-JnN_4_fhk94YWY8yLx2hk0VXS7Ve1wcj8AgNz6z3i5jgUCpLysuToyVF74067smNCV6B-FXL3yAJG3_RPF0wduoi4e2BFXlsZBqCHxVA0QtwblYGPMh4JImz7pU",
                "kty": "RSA",
                "q": "sG_eKMk7Z_5xXRX4tMS3w9MBYj4AbhBP1Pe771Bnh_DGsKIBc6PzgOF5sbqQu1g4LkBU9IAnNVzygtCym-yG4GVx3ys6KbEvwQk_muagDapGFwneRi0CeO0obX3nGEmBMyiZEo0DiYIDkTHmCeaCoBAMi2ibIit6T4qndOnAsvU",
                "d": "H4XfYu-4znSToXA7fOU7086IeUBPhobOo9WDJMrdBYASTts6-M60ANkYGhNnC7BUAjk3GhylYG0PUeTEwlTdi-Nj1bKu3e2bvsVN8KedOOrHGaRh9sMBUk49WOOczxIS-lcP57lJKb1SJUzJAarEq-Yo8v3Kn_Y3kH5eoI62MQ2bELoCBtEeP0jSh4EsRUxZiATbi4EVmk71BE2XlnwDuRQa0FUWsJI9TPMagjj0-T95oN5SWYrHwS606aQCDhsoGMmXhcrO16y7wcFW2gtUrSktoZ3o47DwMsEI_lO4aPpvUfkiHLDLWX_2aEe_3m76qz0YoQ_q3x8krFZtsJLFIQ",
                "e": "AQAB",
                "use": "sig",
                "kid": "RyResLhzSS5p2qUq6tNla_mOkG9vU8oiYNmDEYJ9OGg",
                "qi": "1cL4KozopEnDgs5WqPweGwIdASqv-rlBb09CFRRm3gCQgGDtnzmqXxUSA41M8oerhmCBzO5aaRDeD61In8RlbuuHKz0jNyAIuOYvT0Cjb_qhR_YgjMGaAsQGrDFtpT2t5vLmXmtfd738lAzIGSTwIjyzcmkOVm-Uu6GqnsXNN0o",
                "dp": "ih93ey_LMS9FIdaVw9we2RLGJhuHyvZX93_0_K9eVGBeaeUzpMUYP0-hOwfb93lwU_UQ2_sUEfIHZL9Q9gYjTfqfdnyChcBVOdTNMlb_f-azAnzcSdNdFLBVQ7qIc0GNue7mKA39uaKY7hx-scjMguH2BcShClfhM93YDgf_NsU",
                "alg": "RS256",
                "dq": "EUq1dDvcpaGLTLs1h8nr1LZeYVmAQ0k3-cBZ2-d-bhVL88285K-B0vTGL4QAYUwRHX9SlC_nJ9S3M9juWFfDFET89wlOhv2Fl56nbDAEKbKxcdqWvTv2DRH5L1nyhy_VCMWG0wmc-iIS5W0FhZzg2n0YLaWP5rZqI1eMd-nc5-0",
                "n": "nBtew1u5jVrXdHzTfd3kFlSAIZi3q1FiR87sbf2i7FQuj1S5UT0uiINnUfiEJDM3pPObUfm4_TkPapZ4nMvOpf2YkZgEWEu03_Kgq-IXdJFQNXUGNlZGv1fufv5KuF3R5ELljW2ywmrumjDKskVf4eDBnj_KGj81GBATyaFYKntd53HqMksTdcooqj0JNeU-RYso9gCQhIGjdC-c93vRxa49vnvXBNwU8TJAefhqaK55Ew_HVWXE0Fz_eWEVVuk5uTq58uwtsIfMpMHEXC4USNVg3-gXDW3Yq1H_Tjc1nkdCsNskStS1_leABj4LPImVrRfkNp5sd_JJnbxTcdbumQ"
            }
            """);

        JWSSigner signer = new RSASSASigner(rsaJWK);
        signer.supportedJWSAlgorithms().forEach(System.out::println);

        // https://docs.digdir.no/docs/Maskinporten/maskinporten_protocol_jwtgrant
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .audience("https://test.maskinporten.no/")
            .issuer("826acbbc-ee17-4946-af92-cf4885ebe951") // "eformidling-tenor-test-klient-01" for KUL SLITEN TIGER AS (314240979)
            .claim("scope", "altinn:broker.read altinn:broker.write")
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
            .expirationTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(120L)))
            .build();

        // maskinporten trenger kid eller x5c i header (kid for asymmetrisk nøkkel, x5c for virksomhetssertifikat)
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
            claims
        );

        // sign it with our private key
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

    private String exchangeAltinnToken(String maskinportenToken) throws Exception {
        RestClient restClient = RestClient.create("https://platform.tt02.altinn.no/authentication/api/v1/exchange/maskinporten");
        String token = restClient.get()
            .header("Authorization", "Bearer " + maskinportenToken)
            .retrieve()
            .body(String.class);
        return token;
    }

}
