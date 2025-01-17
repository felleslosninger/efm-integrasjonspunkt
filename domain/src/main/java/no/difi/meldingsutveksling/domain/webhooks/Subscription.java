package no.difi.meldingsutveksling.domain.webhooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.validation.OneOf;
import no.difi.meldingsutveksling.validation.WebhookFilter;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.validator.constraints.URL;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@Getter
@Setter
@ToString
@Entity
@Table(name = "webhook_subscription")
@OptimisticLocking(type = OptimisticLockType.NONE)
public class Subscription extends AbstractEntity<Long> {

    @NotNull(groups = ValidationGroups.Create.class)
    private String name;

    @Null(groups = ValidationGroups.Create.class)
    @Override
    @JsonProperty
    public Long getId() {
        return super.getId();
    }

    @URL
    @NotNull(groups = ValidationGroups.Create.class)
    private String pushEndpoint;

    @NotNull(groups = ValidationGroups.Create.class)
    @OneOf({"all", "messages"})
    private String resource;

    @NotNull(groups = ValidationGroups.Create.class)
    @OneOf({"all", "status"})
    private String event;

    @WebhookFilter
    private String filter;
}
