package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Data
@RequiredArgsConstructor(staticName = "of")
@NoArgsConstructor
@Embeddable
public class SmsVarsel {

    @NonNull
    @Column(name = "sms_tekst")
    private String tekst;
}
