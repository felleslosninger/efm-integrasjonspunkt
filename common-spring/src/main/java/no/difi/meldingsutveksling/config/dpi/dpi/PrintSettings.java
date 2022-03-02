package no.difi.meldingsutveksling.config.dpi.dpi;

import lombok.Data;

@Data
public class PrintSettings {
    private ReturnType returnType;
    private InkType inkType;
    private ShippingType shippingType;
}
