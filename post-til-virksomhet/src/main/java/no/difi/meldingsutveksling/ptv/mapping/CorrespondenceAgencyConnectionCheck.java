package no.difi.meldingsutveksling.ptv.mapping;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;

import jakarta.annotation.PostConstruct;

@RequiredArgsConstructor
public class CorrespondenceAgencyConnectionCheck {

    private final CorrespondenceAgencyClient correspondenceAgencyClient;

    @PostConstruct
    public void checkTheConnection() {
        try {
            Object response = correspondenceAgencyClient.sendTestRequest();

            if (response == null) {
                // Error is picked up by soap fault interceptor
                throw new NextMoveRuntimeException("Null response from CorrespondenceAgency");
            }
        } catch (Exception e) {
            throw new NextMoveRuntimeException("Couldn't connect to CorrespondenceAgency", e);
        }
    }

}
