package no.difi.meldingsutveksling.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Receiver", propOrder = {
        "identifier",
        "name",
        "ref"
})
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class Receiver {

    @XmlAttribute(name = "identifier", required = true)
    private String identifier;
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "ref")
    private String ref;

}
