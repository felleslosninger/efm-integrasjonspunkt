package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DpiNotification {

    String emailText;
    String smsText;
}
