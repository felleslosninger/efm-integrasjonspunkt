package no.difi.meldingsutveksling.ks.svarinn;

import lombok.NonNull;
import lombok.Value;

import java.util.function.Supplier;

@Value(staticConstructor = "of")
public class SvarInnMessage {
    @NonNull
    private final Forsendelse forsendelse;
    @NonNull
    private final Supplier<SvarInnStreamedFile> streamFileSupplier;
}
