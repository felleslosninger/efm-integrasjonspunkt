package no.difi.meldingsutveksling.nextmove;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.domain.BusinessMessage;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "dialogmelding", namespace = "urn:no:difi:digitalpost:json:schema::dialogmelding")
public class Dialogmelding implements BusinessMessage {

    Notat notat;

    String patientFnr;
    String responsibleHealthcareProfessionalId;
    String vedleggBeskrivelse;
    Person patient;
}
