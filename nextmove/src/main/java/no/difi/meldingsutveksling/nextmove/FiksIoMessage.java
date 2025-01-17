package no.difi.meldingsutveksling.nextmove;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "fiksio", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class FiksIoMessage extends BusinessMessage<FiksIoMessage> {
}
