package no.difi.meldingsutveksling.ks.svarinn;

import java.util.List;

public class SvarInnMessageFactory {
    public SvarInnMessage create(Forsendelse forsendelse, List<SvarInnFile> files) {
        return new SvarInnMessage(forsendelse, files);
    }
}
