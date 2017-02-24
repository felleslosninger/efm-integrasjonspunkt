package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Data;
import lombok.NonNull;
import no.difi.meldingsutveksling.core.EDUCore;

@Data
public class SvarInnMessage {
    @NonNull
    SvarInnFile svarInnFile;
    @NonNull
    Forsendelse forsendelse;

    EDUCore toEduCore() {
        return null;
    }
}
