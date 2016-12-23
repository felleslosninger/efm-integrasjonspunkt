package no.difi.meldingsutveksling.ks.mapping.edu;

import com.google.common.base.Objects;
import no.difi.meldingsutveksling.ks.Dokument;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.NoarksakType;

import java.util.List;

/**
 * Used to map EDU MeldingType to Forsendelse
 */
public class MeldingTypeHandler implements Handler<Forsendelse.Builder> {
    private final MeldingType meldingType;

    public MeldingTypeHandler(MeldingType meldingType) {
        this.meldingType = meldingType;
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        final List<DokumentType> dokumentTypes = meldingType.getJournpost().getDokument();
        final NoarksakType noarksak = meldingType.getNoarksak();
        builder.withTittel(noarksak.getSaOfftittel());
        for (DokumentType d : dokumentTypes) {
            Dokument.Builder dokumentBuilder = new DokumentTypeHandler(d).map(Dokument.builder());
            builder.addDokumenter(dokumentBuilder.build());
        }
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeldingTypeHandler)) return false;
        MeldingTypeHandler that = (MeldingTypeHandler) o;
        return Objects.equal(meldingType, that.meldingType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(meldingType);
    }
}
