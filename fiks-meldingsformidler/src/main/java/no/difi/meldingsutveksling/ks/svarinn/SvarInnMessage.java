package no.difi.meldingsutveksling.ks.svarinn;

import lombok.NonNull;
import lombok.Value;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;

import java.util.function.Supplier;

@Value(staticConstructor = "of")
public class SvarInnMessage {
    @NonNull
    Forsendelse forsendelse;
    @NonNull
    Supplier<Document> documentSupplier;
}
