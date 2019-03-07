package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
@ToString
public class BusinessMessageFile extends AbstractEntity<Long> {

    @NonNull
    private String identifier;
    @NonNull
    private String filename;
    private Boolean primaryDocument;
    private String mimetype;
    private String title;
}
