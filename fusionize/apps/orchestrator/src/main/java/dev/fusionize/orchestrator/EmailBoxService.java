package dev.fusionize.orchestrator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EmailBoxService {
    private final List<String> inbox = new CopyOnWriteArrayList<>();

    public void addInbox(String email) {
        inbox.add(email);
    }

    public List<String> getInbox() {
        return inbox;
    }
}
