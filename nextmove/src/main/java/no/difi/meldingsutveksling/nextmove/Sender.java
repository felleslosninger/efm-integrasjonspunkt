package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Embeddable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Sender {

    private String senderId;
    private String senderName;

    public static Sender of(String senderId, String senderName) {
        return new Sender(senderId, senderName);
    }
}
