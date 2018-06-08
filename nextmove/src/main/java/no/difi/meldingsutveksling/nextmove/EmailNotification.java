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
public class EmailNotification {

    @NonNull
    @Column(name = "email_text")
    private String text;
}
