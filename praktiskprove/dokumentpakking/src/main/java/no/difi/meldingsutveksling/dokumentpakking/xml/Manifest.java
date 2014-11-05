package no.difi.meldingsutveksling.dokumentpakking.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Manifest", propOrder = {
 "mottaker",
 "avsender",
 "hoveddokument",
})
@XmlRootElement(name = "manifest")
public class Manifest {

 @XmlElement(required = true)
 protected String mottaker;
 @XmlElement(required = true)
 protected String avsender;
 @XmlElement(required = true)
 protected String hoveddokument;
 
 public String getMottaker() {
     return mottaker;
 }

 public void setMottaker(String value) {
     this.mottaker = value;
 }

 public String getAvsender() {
     return avsender;
 }

 public void setAvsender(String value) {
     this.avsender = value;
 }

 public String getHoveddokument() {
     return hoveddokument;
 }

 public void setHoveddokument(String value) {
     this.hoveddokument = value;
 }

}
