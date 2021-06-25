package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PeekNextMoveMessageIn {

    List<Long> findIdsForUnlockedMessages(NextMoveInMessageQueryInput input, int maxResults);

    Optional<NextMoveInMessage> lock(long id, OffsetDateTime lockTimeout);
}
