package no.difi.meldingsutveksling.nextmove.v2;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.nextmove.QNextMoveInMessage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PageRequests {

    public static final PageRequest FIRST_BY_LAST_UPDATED_ASC = new PageRequest(0, 1,
            Sort.Direction.ASC,
            QNextMoveInMessage.nextMoveInMessage.lastUpdated.getMetadata().getName());
}
