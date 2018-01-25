package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Embeddable
public class Receiver {

    @NonNull
    private String receiverId;
    private String receiverName;
}
