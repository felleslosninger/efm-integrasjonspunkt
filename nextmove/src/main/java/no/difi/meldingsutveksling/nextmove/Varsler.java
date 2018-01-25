package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Data
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
@Embeddable
public class Varsler {

    @Embedded
    @NonNull
    private EpostVarsel epostVarsel;
    @Embedded
    @NonNull
    private SmsVarsel smsVarsel;
}
