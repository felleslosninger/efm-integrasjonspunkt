package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class FileAttachement {

    @Id
    @GeneratedValue
    @JsonIgnore
    private Integer faId;
    private String filnavn;
    private String tittel;
    private String mimetype;
    private boolean isHoveddokument;
}
