package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OnBehalfOfNormalizerTest {

    @Mock
    private IntegrasjonspunktProperties properties;

    @InjectMocks
    private OnBehalfOfNormalizer target;

    @ParameterizedTest
    @CsvSource({
            "0192:910075918, 0192:910075918",
            "0192:111111111, 0192:910075918:111111111",
            "0192:910075918:111111111, 0192:910075918:111111111"
    })
    void normalize(String before, String after) {
        lenient().when(properties.getOrg()).thenReturn(
                new IntegrasjonspunktProperties.Organization()
                        .setNumber("910075918")
        );

        StandardBusinessDocument sbd = new StandardBusinessDocument()
                .setSenderIdentifier(PartnerIdentifier.parse(before));
        target.normalize(sbd);
        assertThat(sbd.getSenderIdentifier()).isEqualTo(PartnerIdentifier.parse(after));
    }
}
