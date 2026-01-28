package no.difi.meldingsutveksling.fiks.svarinn;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.core.io.Resource;

public class SvarInnPackage {

    private final StandardBusinessDocument sbd;
    private final Resource asic;

    public SvarInnPackage(StandardBusinessDocument sbd, Resource asic) {
        this.sbd = sbd;
        this.asic = asic;
    }

    public StandardBusinessDocument getSbd() {
        return sbd;
    }

    public Resource getAsic() {
        return asic;
    }

}
