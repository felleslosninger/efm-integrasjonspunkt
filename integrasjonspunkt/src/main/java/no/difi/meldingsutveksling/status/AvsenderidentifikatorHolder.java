package no.difi.meldingsutveksling.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class AvsenderidentifikatorHolder {

    private final IntegrasjonspunktProperties properties;
    @Getter(lazy = true) private final Set<String> avsenderidentifikatorListe = fetchAvsenderidentifikatorListe();

    private Set<String> fetchAvsenderidentifikatorListe() {
        return Optional.ofNullable(properties)
                .flatMap(p -> Optional.ofNullable(p.getDpi()))
                .flatMap(p -> Optional.ofNullable(p.getAvsenderidentifikatorListe()))
                .map(p -> Collections.unmodifiableSet(new HashSet<>(p)))
                .orElseGet(Collections::emptySet);
    }

    public boolean pollWithoutAvsenderidentifikator() {
        return true;
    }
}
