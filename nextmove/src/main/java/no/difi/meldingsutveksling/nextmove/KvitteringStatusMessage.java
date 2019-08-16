package no.difi.meldingsutveksling.nextmove;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "kvittering_status_message")
public class KvitteringStatusMessage extends AbstractEntity<Long> {
    private String code;
    private String text;
}
