package no.difi.meldingsutveksling.status;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class MpcIdHolder {

    private final IntegrasjonspunktProperties properties;
    private final AtomicInteger index = new AtomicInteger(0);
    @Getter(lazy = true) private final List<String> mpcIdList = fetchMpcIdList();

    private List<String> fetchMpcIdList() {
        DigitalPostInnbyggerConfig dpi = properties.getDpi();

        if (dpi.getMpcIdListe() != null) {
            return dpi.getMpcIdListe();
        }

        int mpcConcurrency = dpi.getMpcConcurrency();

        if (mpcConcurrency > 1) {
            return IntStream.range(0, mpcConcurrency)
                    .mapToObj(p -> dpi.getMpcId() + "-" + p)
                    .collect(collectingAndThen(toList(), ImmutableList::copyOf));
        } else {
            return Collections.singletonList(dpi.getMpcId());
        }
    }

    public String getNextMpcId() {
        List<String> ids = getMpcIdList();
        int i = index.getAndIncrement() % ids.size();
        return ids.get(i);
    }
}
