package no.difi.meldingsutveksling.ptp;

import net.logstash.logback.marker.Markers;
import no.difi.ptp.oppslagstjenesten.HentPersonerForespoersel;
import no.difi.ptp.oppslagstjenesten.Informasjonsbehov;
import no.difi.webservice.support.SoapFaultInterceptorLogger;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.Merlin;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j.support.CryptoFactoryBean;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.Arrays;

public class OppslagstjenesteClient {
    public void hentKontaktInformasjon(String pid) {
        final HentPersonerForespoersel hentPersonerForespoersel = HentPersonerForespoersel.builder().
                addInformasjonsbehov(Informasjonsbehov.KONTAKTINFO)
                .addPersonidentifikator(pid)
                .build();

        AxiomSoapMessageFactory messageFactory = new AxiomSoapMessageFactory();
        messageFactory.setSoapVersion(SoapVersion.SOAP_12);
        WebServiceTemplate template = new WebServiceTemplate(messageFactory);

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("no.difi.ptp.oppslagstjenesten");
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        final ClientInterceptor[] interceptors = new ClientInterceptor[2];
        interceptors[0] = createSecurityInterceptor();
        interceptors[1] = SoapFaultInterceptorLogger.withLogMarkers(Markers.append("some marker", ""));

        template.setInterceptors(interceptors);

        final Object result = template.marshalSendAndReceive("https://kontaktinfo-ws-ver2.difi.no/kontaktinfo-external/ws-v5", hentPersonerForespoersel);
    }


    private ClientInterceptor createSecurityInterceptor() {
        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementActions("Signature Timestamp");
        securityInterceptor.setFutureTimeToLive(120);
        securityInterceptor.setSecurementSignatureKeyIdentifier("DirectReference");
        securityInterceptor.setSecurementUsername("client_alias");
        securityInterceptor.setSecurementPassword("changeit");
        securityInterceptor.setValidationActions("Signature Timestamp Encrypt");

        securityInterceptor.setValidationCallbackHandler(new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                Arrays.stream(callbacks).filter(c -> c instanceof WSPasswordCallback).forEach(c -> ((WSPasswordCallback)c).setPassword("changeit"));
            }
        });

        securityInterceptor.setSecurementSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        securityInterceptor.setSecurementSignatureParts("{}{http://www.w3.org/2003/05/soap-envelope}Body;{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp}");
        final Crypto clientCrypto = getCryptoFactoryBean(new FileSystemResource("kontaktinfo-client-test.jks"), "changeit", "client_alias");
        final Crypto serverCrypto = getCryptoFactoryBean(new FileSystemResource("kontaktinfo-server-test.jks"), "changeit", "ver2");
        securityInterceptor.setSecurementSignatureCrypto(clientCrypto);
        securityInterceptor.setValidationSignatureCrypto(serverCrypto);
        securityInterceptor.setValidationDecryptionCrypto(clientCrypto);
        securityInterceptor.setSecurementEncryptionCrypto(clientCrypto);

        return securityInterceptor;
    }

    private Crypto getCryptoFactoryBean(Resource keystore, String password, String alias) {
        final CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        try {
            cryptoFactoryBean.setKeyStoreLocation(keystore);
            cryptoFactoryBean.setKeyStoreType("jks");
            cryptoFactoryBean.setCryptoProvider(Merlin.class);
            cryptoFactoryBean.setKeyStorePassword(password);
            cryptoFactoryBean.setDefaultX509Alias(alias);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create security interceptor due to problems with keystore file...", e);
        }
        try {
            cryptoFactoryBean.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("ERROR", e);
        }
        try {
            return cryptoFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Could not create CryptoFactoryBean", e);
        }
    }
}
