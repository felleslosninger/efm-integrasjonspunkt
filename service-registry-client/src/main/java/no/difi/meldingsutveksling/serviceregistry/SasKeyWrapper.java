package no.difi.meldingsutveksling.serviceregistry;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SasKeyWrapper {

    @Id
    @Column(length = 45)
    String saskey;
}
