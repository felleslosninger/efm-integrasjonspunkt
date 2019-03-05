package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BusinessMessage {
    private String securityLevel;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "fileid")
    @Column(name = "file")
    @CollectionTable(
            name = "nextmove_files",
            joinColumns = @JoinColumn(name = "id")
    )
    private Map<String, String> files;
}
