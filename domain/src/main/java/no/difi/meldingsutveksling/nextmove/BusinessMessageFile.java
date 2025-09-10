package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@ToString
@Table(name="business_message_file", indexes = {
        @Index(name="idxMessageId", columnList = "message_id")
}
)
public class BusinessMessageFile extends AbstractEntity<Long> {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private NextMoveMessage message;

    @NonNull
    private String identifier;
    @NonNull
    private String filename;
    private Boolean primaryDocument;
    private String mimetype;
    private String title;
    @NonNull
    private Integer dokumentnummer;
    @NonNull
    private Long size;
}
