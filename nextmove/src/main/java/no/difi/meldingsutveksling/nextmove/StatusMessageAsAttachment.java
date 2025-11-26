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
public class StatusMessageAsAttachment extends BusinessMessageAsAttachment<StatusMessageAsAttachment> {
    private ReceiptStatus status;
}
