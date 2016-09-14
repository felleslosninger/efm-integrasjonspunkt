package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.*;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.exceptions.SendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.security.KeyStore;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MeldingsformidlerClient {
    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String URL_MELDINGS_FORMIDLER = "https://qaoffentlig.meldingsformidler.digipost.no/api/ebms";
    private final Config config;


    public MeldingsformidlerClient(Config config) {
        this.config = config;
    }

    public void sendMelding(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        KontaktInfo kontaktInfo = new KontaktInfo(); // this should come from SR once we're done
        Mottaker mottaker = Mottaker.builder(request.getMottakerPid(), kontaktInfo.getPostkasseAdresse(), Sertifikat.fraByteArray(kontaktInfo.getCertificate()), kontaktInfo.getOrgnrPostkasse()).build();
        DigitalPost digitalPost = DigitalPost.builder(mottaker, request.getSubject()).virkningsdato(new Date()).build();
        Dokument dokument = Dokument.builder(request.getDocumentTitle(), request.getDocumentName(), request.getDocument()).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).build(); // skal dokumentpakke ha vedlegg?
        Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder(request.getSenderOrgnumber()).build();

        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost, dokumentpakke)
                .konversasjonsId(request.getConversationId()) // fra integrasjonspunkt?
//                .mpcId(request.getQueueId()) // køid? unik for å unngå kollisjon med andre avsendere
//                .prioritet(Prioritet.NORMAL) // eller Prioritet.Prioritert?
//                .spraakkode(request.getSpraakKode())
                .build();

        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder().meldingsformidlerRoot(config.getUrl()).connectionTimeout(20, TimeUnit.SECONDS).build();


        TekniskAvsender tekniskAvsender = TekniskAvsender.builder(request.getSenderOrgnumber(), Noekkelpar.fraKeyStoreUtenTrustStore(config.getKeyStore(), config.getKeystoreAlias(), config.getKeystorePassword())).build();

        SikkerDigitalPostKlient klient = new SikkerDigitalPostKlient(tekniskAvsender, klientKonfigurasjon);
        try {
            klient.send(forsendelse);
        } catch (SendException e) {
            throw new MeldingsformidlerException("Unable to send message to SDP", e);
        }
    }

    public static class Config {
        private final String orgnumber;
        private final String url;
        private KeyStore keyStore;
        private String keystoreAlias;
        private String keystorePassword;

        public Config(String orgnumber, String url, KeyStore keyStore, String keystoreAlias, String keystorePassword) {
            this.orgnumber = orgnumber;
            this.url = url;
            this.keyStore = keyStore;
            this.keystoreAlias = keystoreAlias;
            this.keystorePassword = keystorePassword;
        }

        public String getOrgnumber() {
            return orgnumber;
        }

        public String getUrl() {
            return url;
        }

        public KeyStore getKeyStore() {
            return keyStore;
        }

        public String getKeystoreAlias() {
            return keystoreAlias;
        }

        public String getKeystorePassword() {
            return keystorePassword;
        }
    }

    private class KontaktInfo {

        public String getPostkasseAdresse() {
            return "test.testesen#2HQF";
        }

        public byte[] getCertificate() {
            try {
                return "-----BEGIN CERTIFICATE-----\nMIIE7jCCA9agAwIBAgIKGBZrmEgzTHzeJjANBgkqhkiG9w0BAQsFADBRMQswCQYDVQQGEwJOTzEdMBsGA1UECgwUQnV5cGFzcyBBUy05ODMxNjMzMjcxIzAhBgNVBAMMGkJ1eXBhc3MgQ2xhc3MgMyBUZXN0NCBDQSAzMB4XDTE0MDQyNDEyMzA1MVoXDTE3MDQyNDIxNTkwMFowVTELMAkGA1UEBhMCTk8xGDAWBgNVBAoMD1BPU1RFTiBOT1JHRSBBUzEYMBYGA1UEAwwPUE9TVEVOIE5PUkdFIEFTMRIwEAYDVQQFEwk5ODQ2NjExODUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCLCxU4oBhtGmJxXZWbdWdzO2uA3eRNW/kPdddL1HYl1iXLV/g+H2Q0ELadWLggkS+1kOd8/jKxEN++biMmmDqqCWbzNdmEd1j4lctSlH6M7tt0ywmXIYdZMz5kxcLAMNXsaqnPdikI9uPJZQEL3Kc8hXhXISvpzP7gYOvKHg41uCxu1xCZQOM6pTlNbxemBYqvES4fRh2xvB9aMjwkB4Nz8jrIsyoPI89i05OmGMkI5BPZt8NTa40Yf3yU+SQECW0GWalB5cxaTMeB01tqslUzBJPV3cQx+AhtQG4hkOhQnAMDJramSPVtwbEnqOjQ+lyNmg5GQ4FJO02ApKJTZDTHAgMBAAGjggHCMIIBvjAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBQ1gsJfVC7KYGiWVLP7ZwzppyVYTTAOBgNVHQ8BAf8EBAMCBLAwFgYDVR0gBA8wDTALBglghEIBGgEAAwIwgbsGA1UdHwSBszCBsDA3oDWgM4YxaHR0cDovL2NybC50ZXN0NC5idXlwYXNzLm5vL2NybC9CUENsYXNzM1Q0Q0EzLmNybDB1oHOgcYZvbGRhcDovL2xkYXAudGVzdDQuYnV5cGFzcy5uby9kYz1CdXlwYXNzLGRjPU5PLENOPUJ1eXBhc3MlMjBDbGFzcyUyMDMlMjBUZXN0NCUyMENBJTIwMz9jZXJ0aWZpY2F0ZVJldm9jYXRpb25MaXN0MIGKBggrBgEFBQcBAQR+MHwwOwYIKwYBBQUHMAGGL2h0dHA6Ly9vY3NwLnRlc3Q0LmJ1eXBhc3Mubm8vb2NzcC9CUENsYXNzM1Q0Q0EzMD0GCCsGAQUFBzAChjFodHRwOi8vY3J0LnRlc3Q0LmJ1eXBhc3Mubm8vY3J0L0JQQ2xhc3MzVDRDQTMuY2VyMA0GCSqGSIb3DQEBCwUAA4IBAQCe67UOZ/VSwcH2ov1cOSaWslL7JNfqhyNZWGpfgX1c0Gh+KkO3eVkMSozpgX6M4eeWBWJGELMiVN1LhNaGxBU9TBMdeQ3SqK219W6DXRJ2ycBtaVwQ26V5tWKRN4UlRovYYiY+nMLx9VrLOD4uoP6fm9GE5Fj0vSMMPvOEXi0NsN+8MUm3HWoBeUCLyFpe7/EPsS/Wud5bb0as/E2zIztRodxfNsoiXNvWaP2ZiPWFunIjK1H/8EcktEW1paiPd8AZek/QQoG0MKPfPIJuqH+WJU3a8J8epMDyVfaek+4+l9XOeKwVXNSOP/JSwgpOJNzTdaDOM+uVuk75n2191Fd7\n-----END CERTIFICATE-----\n".getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public String getOrgnrPostkasse() {
            return "984661185";
        }
    }
}
