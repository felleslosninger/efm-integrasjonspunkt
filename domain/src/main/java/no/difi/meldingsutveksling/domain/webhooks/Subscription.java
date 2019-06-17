package no.difi.meldingsutveksling.domain.webhooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import no.difi.meldingsutveksling.nextmove.AbstractEntity;
import no.difi.meldingsutveksling.validation.OneOf;
import no.difi.meldingsutveksling.validation.WebhookFilter;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

@Getter
@Setter
@ToString
@Entity
@Table(name = "webhook_subscription")
@ApiModel(description = "Webhook subscription")
public class Subscription extends AbstractEntity<Long> {

    @NotNull(groups = ValidationGroups.Create.class)
    @ApiModelProperty(
            position = 1,
            value = "A label to remember why it was created. Use it for whatever purpose you'd like.",
            required = true,
            example = "Outgoing DPO messages"
    )
    private String name;

    @Null(groups = ValidationGroups.Create.class)
    @Override
    @JsonProperty
    @ApiModelProperty(
            position = 2,
            value = "Id",
            example = "1")
    public Long getId() {
        return super.getId();
    }

    @NotNull(groups = ValidationGroups.Create.class)
    @Column(unique = true)
    @ApiModelProperty(
            position = 3,
            value = "URL to push the webhook messages too",
            required = true,
            example = "https://webhook.site/bb8bb41a-c559-47ed-84f1-1846ece5d590"
    )
    private String pushEndpoint;

    @NotNull(groups = ValidationGroups.Create.class)
    @OneOf({"all", "messages"})
    @ApiModelProperty(
            position = 4,
            value = "Indicates the noun being observed.",
            required = true,
            allowableValues = "all,messages",
            example = "all"
    )
    private String resource;

    @NotNull(groups = ValidationGroups.Create.class)
    @OneOf({"all", "status"})
    @ApiModelProperty(
            position = 5,
            value = "Further narrows the events by specifying the action that would trigger a notification to your backend.",
            required = true,
            allowableValues = "all,status",
            example = "status"
    )
    private String event;

    @WebhookFilter
    @ApiModelProperty(
            position = 6,
            value = "A set of filtering critera. Generally speaking, webhook filters will be a subset of the query parameters available when GETing a list of the target resource. It is an optional property. To add multiple filters, separate them with the “&” symbol. Supported filters are: status, serviceIdentifier, direction.",
            example = "status=SENDT,LEVERT&direction=OUTGOING&serviceIdentifier=DPO"
    )
    private String filter;
}
