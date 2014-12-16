package no.difi.meldingsutveksling.dokumentpakking.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "content", namespace = "urn:no:difi:meldingsutveksling:1.0")
public class Content {
		
		@XmlValue
		String content;
		
		public Content(String content) {
			this.content = content;
		}
		
		public Content() {
			
		}

		public String getContent() {
			return content;
		}

		
}
