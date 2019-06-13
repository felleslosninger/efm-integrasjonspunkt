package no.difi.meldingsutveksling.domain.webhooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiParam;
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
@ApiModel(description = "Webhook subscription")
public class Subscription extends AbstractEntity<Long> {

    @Override
    @JsonProperty
    @ApiParam(value = "Id")
    public Long getId() {
        return super.getId();
    }

    @NotNull
    @Column(unique = true)
    @ApiParam(value = "URL to push the webhook messages too")
    private String pushEndpoint;
}
