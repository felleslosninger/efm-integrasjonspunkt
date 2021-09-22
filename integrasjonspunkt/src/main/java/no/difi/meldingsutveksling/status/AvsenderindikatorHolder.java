package no.difi.meldingsutveksling.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class AvsenderindikatorHolder {

    private final IntegrasjonspunktProperties properties;
    @Getter(lazy = true) private final Set<String> avsenderindikatorListe = fetchAvsenderindikatorListe();

    private Set<String> fetchAvsenderindikatorListe() {
        return Optional.ofNullable(properties)
                .flatMap(p -> Optional.ofNullable(p.getDpi()))
                .flatMap(p -> Optional.ofNullable(p.getAvsenderindikatorListe()))
                .map(p -> Collections.unmodifiableSet(new HashSet<>(p)))
                .orElseGet(Collections::emptySet);
    }
}
