package no.difi.meldingsutveksling.dph.client;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.internal.DphClient;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.dph.client.internal.WrappedPackage;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class DphClientServiceTest {

    private static final Iso6523 ON_BEHALF_OF = Iso6523.of(ICD.NO_ORG, "991825827");

    @Mock
    private DphClient dphClient;
    @Mock
    private DphParcelService parcelService;

    private DphClientService target;
    private Resource asic;

    @BeforeEach
    void setUp() {
        target = new DphClientService(dphClient, parcelService);
        asic = mock(Resource.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(parcelService.signAndEncrypt(anyString())).thenReturn("request");
        when(dphClient.receiveBusinessDocument(any(), anyString())).thenReturn(new WrappedPackage("forretningsmelding", asic));
    }

    @Test
    void receiveBusinessDocument_closes_the_asic_when_the_business_document_cannot_be_verified() throws Exception {
        when(parcelService.decryptAndVerify("forretningsmelding"))
            .thenThrow(new DphException(FeilmeldingForApplikasjonskvittering.SIGNATURFEIL));

        assertThatExceptionOfType(DphException.class)
            .isThrownBy(() -> target.receiveBusinessDocument(ON_BEHALF_OF, "message"));

        verify((AutoCloseable) asic).close();
    }

    @Test
    void receiveBusinessDocument_does_not_blame_the_message_for_business_document_that_cannot_be_deserialized_and_closes_the_asic() throws Exception {
        JWSObject jws = mock(JWSObject.class);
        when(jws.getPayload()).thenReturn(new Payload("{}"));
        when(parcelService.decryptAndVerify("forretningsmelding")).thenReturn(jws);
        when(parcelService.toSBD("{}")).thenThrow(new IllegalArgumentException("Deserializing from JSON to SBD failed"));

        assertThatExceptionOfType(DphException.class)
            .isThrownBy(() -> target.receiveBusinessDocument(ON_BEHALF_OF, "message"))
            .satisfies(e -> {
                assertThat(e).isNotInstanceOf(InvalidBusinessDocumentException.class);
                assertThat(e.getErrorCode()).isEqualTo(FeilmeldingForApplikasjonskvittering.ANNEN_FEIL.getVerdi());
                assertThat(e.getStatusCode()).isNull();
            });

        verify((AutoCloseable) asic).close();
    }
}
