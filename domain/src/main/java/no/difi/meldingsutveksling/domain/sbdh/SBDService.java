package no.difi.meldingsutveksling.domain.sbdh;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SBDService {

    private final Clock clock;

    public boolean isExpired(StandardBusinessDocument sbd) {
        return sbd.getExpectedResponseDateTime()
                .map(this::isExpired)
                .orElse(false);
    }

    private boolean isExpired(OffsetDateTime expectedResponseDateTime) {
        OffsetDateTime currentDateTime = OffsetDateTime.now(clock);
        return currentDateTime.isAfter(expectedResponseDateTime);
    }
}
