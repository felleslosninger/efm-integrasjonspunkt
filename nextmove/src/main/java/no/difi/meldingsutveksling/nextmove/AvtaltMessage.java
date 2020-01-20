package no.difi.meldingsutveksling.nextmove;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "avtalt", namespace = "urn:no:difi:meldingsutveksling:2.0")
@ApiModel(value = "avtalt", parent = BusinessMessage.class)
public class AvtaltMessage extends BusinessMessage<AvtaltMessage> {
}