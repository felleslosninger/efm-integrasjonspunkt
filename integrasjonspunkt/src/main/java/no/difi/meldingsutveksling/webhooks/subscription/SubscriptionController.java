package no.difi.meldingsutveksling.webhooks.subscription;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@Validated
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Transactional
    public Page<Subscription> listSubscriptions(@PageableDefault Pageable pageable) {
        return subscriptionService.listSubscriptions(pageable);
    }

    @GetMapping("{id}")
    @Transactional
    public Subscription getSubscription(@PathVariable @NotNull Long id) {
        return subscriptionService.getSubscription(id);
    }

    @PostMapping
    @Transactional
    public Subscription createSubscription(@Validated(ValidationGroups.Create.class) @RequestBody Subscription subscription) {
        return subscriptionService.createSubscription(subscription);
    }

    @PutMapping("{id}")
    @Transactional
    public void updateSubscription(@PathVariable @NotNull Long id,
                                   @Validated(ValidationGroups.Update.class) @RequestBody Subscription subscription) {
        subscriptionService.updateSubscription(id, subscription);
    }

    @DeleteMapping("{id}")
    @Transactional
    public void deleteSubscription(@PathVariable @NotNull Long id) {

        subscriptionService.deleteSubscription(id);
    }

    @DeleteMapping
    @Transactional
    public void deleteAllSubscriptions() {
        subscriptionService.deleteAll();
    }
}
