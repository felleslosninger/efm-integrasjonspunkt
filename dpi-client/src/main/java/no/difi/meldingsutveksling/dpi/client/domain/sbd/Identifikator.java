
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Identifikator {

    private String authority;
    private String value;
}
