package no.difi.meldingsutveksling.nextmove;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "dialogmelding", namespace = "urn:no:difi:digitalpost:json:schema::dialogmelding")
public class Dialogmelding extends BusinessMessage<Dialogmelding> {

    Notat notat;
    String patientFnr;
    String responsibleHealthcareProfessionalId;
    String vedleggBeskrivelse;

}
