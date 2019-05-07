package no.difi.meldingsutveksling.domain.webhooks;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Entity
@Table(name = "webhook_subscription")
public class Subscription extends AbstractEntity<Long> {

    @NotNull
    @Column(unique = true)
    private String pushEndpoint;
}
