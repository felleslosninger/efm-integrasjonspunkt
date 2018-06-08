package no.difi.meldingsutveksling.nextmove.dpi;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.nextmove.Notifications;
import no.difi.meldingsutveksling.xml.LocalDateAdapter;
import no.difi.meldingsutveksling.xml.LocalTimeAdapter;

import javax.persistence.Embeddable;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class DigitalPostInfo {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    private LocalDate virkningsdato;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private LocalTime virkningstidspunkt;
    private Boolean aapningskvittering;
    private String ikkeSensitivTittel;
    private Notifications notifications;
}
