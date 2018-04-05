package no.difi.meldingsutveksling.config.dpi;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintSettings {

    private ReturnType returnType;
    private InkType inkType;
    private ShippingType shippingType;

}
