package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class NotificationTest {
    @Test
    public void whenNotificationIsObligatedThenCreateQueryStringShouldContainNotificationObligated() {
        Notification obligated = Notification.OBLIGATED;

        assertThat(obligated.createQuery(), containsString("notification"));
        assertThat(obligated.createQuery(), containsString("obligated"));
    }

    @Test
    public void whenNotificationIsObligatedThenCreateQueryStringShouldContainNotificationNot_Obligated() {
        Notification notObligated = Notification.NOT_OBLIGATED;

        assertThat(notObligated.createQuery(), containsString("notification"));
        assertThat(notObligated.createQuery(), containsString("not_obligated"));
    }

}