package dev.fusionize.user.activity;

import java.time.ZonedDateTime;

public class UserAction<G> {
    protected String user;
    protected boolean system = false;
    protected ZonedDateTime date;
    protected G action;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public G getAction() {
        return action;
    }

    public void setAction(G action) {
        this.action = action;
    }
}
