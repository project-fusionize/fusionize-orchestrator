package dev.fusionize.orchestrator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailBoxServiceTest {

    @Test
    void shouldStartWithEmptyInbox() {
        // setup
        var service = new EmailBoxService();

        // expectation
        var inbox = service.getInbox();

        // validation
        assertThat(inbox).isEmpty();
    }

    @Test
    void shouldAddEmailToInbox() {
        // setup
        var service = new EmailBoxService();

        // expectation
        service.addInbox("test@email.com");

        // validation
        assertThat(service.getInbox()).containsExactly("test@email.com");
    }

    @Test
    void shouldAddMultipleEmails() {
        // setup
        var service = new EmailBoxService();

        // expectation
        service.addInbox("a@email.com");
        service.addInbox("b@email.com");
        service.addInbox("c@email.com");

        // validation
        assertThat(service.getInbox()).hasSize(3);
    }

    @Test
    void shouldReturnInboxInOrder() {
        // setup
        var service = new EmailBoxService();

        // expectation
        service.addInbox("a");
        service.addInbox("b");
        service.addInbox("c");

        // validation
        assertThat(service.getInbox()).containsExactly("a", "b", "c");
    }
}
