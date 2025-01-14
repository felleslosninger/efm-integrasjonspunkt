package no.difi.meldingsutveksling.dpi.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelNormalizerTest {

    private final ChannelNormalizer target = new ChannelNormalizer();

    @Test
    void testNormalize() {
        assertThat(target.normalize("no.difi.move.integrasjonspunkt")).isEqualTo("no_difi_move_integrasjonspunkt");
    }
}