package no.difi.meldingsutveksling.serviceregistry;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SasKeyWrapper {

    @Id
    @Column(length = 45)
    String saskey;
}
