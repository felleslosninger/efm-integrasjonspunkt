package no.difi.meldingsutveksling.core;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Used by {@link EDUCore}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Sender", propOrder = {
        "identifier",
        "name",
        "ref"
})
@Data
@AllArgsConstructor(staticName = "of")
public class Sender {

    @XmlAttribute(name = "identifier", required = true)
    private String identifier;
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "ref")
    private String ref;

}
