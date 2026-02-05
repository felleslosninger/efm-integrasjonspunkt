package no.difi.meldingsutveksling.nextmove;

import lombok.*;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import jakarta.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "status", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class StatusMessage extends BusinessMessageAsAttachment<StatusMessage> {
    private ReceiptStatus status;
}
