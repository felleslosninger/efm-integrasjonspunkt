package no.difi.meldingsutveksling.receipt.strategy;

import lombok.extern.slf4j.Slf4j;
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;
import static no.difi.meldingsutveksling.receipt.strategy.DpvStatusStrategy.mapCorrespondenceStatusToReceiptStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class DpvStatusStrategyTest {

    @Test
    void verifyMappings() {

        // verify explicit / edge cases
        assertEquals(ANNET, mapCorrespondenceStatusToReceiptStatus(null));
        assertEquals(LEVERT, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.PUBLISHED));
        assertEquals(LEST, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.READ));
        assertEquals(null, mapCorrespondenceStatusToReceiptStatus(CorrespondenceStatusExt.READY_FOR_PUBLISH));

        // all except the following list should be mapped to ANNET
        var mappedStatuses = List.of(
            CorrespondenceStatusExt.PUBLISHED,
            CorrespondenceStatusExt.READ,
            CorrespondenceStatusExt.READY_FOR_PUBLISH
        );
        Arrays.stream(CorrespondenceStatusExt.values())
            .filter(s -> !mappedStatuses.contains(s))
            .map(DpvStatusStrategy::mapCorrespondenceStatusToReceiptStatus)
            .forEach(s -> assertEquals(ANNET, s)
        );

    }

}
