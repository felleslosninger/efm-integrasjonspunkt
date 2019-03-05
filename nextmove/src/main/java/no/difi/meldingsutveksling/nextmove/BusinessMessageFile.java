package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
@Builder
public class BusinessMessageFile {

    @Id
    @GeneratedValue
    @JsonIgnore
    private String id;

    @NonNull
    private String identifier;
    @NonNull
    private String filename;
    private Boolean primaryDocument;
    private String mimetype;
    private String title;
}
