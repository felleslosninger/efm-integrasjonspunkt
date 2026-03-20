package no.difi.meldingsutveksling.ks.fiksio;

import no.ks.fiks.io.client.model.MottattMelding;
import org.springframework.core.io.AbstractResource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;

public class MottattMeldingResource extends AbstractResource {

    private final MottattMelding mottattMelding;

    public MottattMeldingResource(MottattMelding mottattMelding) {
        this.mottattMelding = mottattMelding;
    }

    @NonNull
    @Override
    public String getDescription() {
        return "MottattMeldingResource";
    }

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        return mottattMelding.getKryptertStream();
    }

}
