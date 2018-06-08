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
public class Notifications {

    @Embedded
    @NonNull
    private EmailNotification emailNotification;
    @Embedded
    @NonNull
    private SmsNotification smsNotification;
}
