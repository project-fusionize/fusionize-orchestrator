package dev.fusionize.user.activity;

import java.time.ZonedDateTime;
import java.util.Map;

public class UserActivity extends UserAction<ActivityType> {
    private Map<String,Object> changes;

    @Override
    public String toString() {
        return "UserActivity{" +
                "system=" + system +
                ", user='" + user + '\'' +
                ", date=" + date +
                ", changes=" + changes +
                ", action=" + action +
                '}';
    }

    public static Builder builder(String userId){
        return new Builder(userId);
    }

    public static final class Builder {
        private final UserActivity formingActivity;

        private Builder(String userId){
            this.formingActivity = new UserActivity();
            formingActivity.user = userId;
            formingActivity.date = ZonedDateTime.now();
            formingActivity.action = ActivityType.UPDATE;
        }

        public Builder changes(Map<String,Object> changes) {
            formingActivity.changes = changes;
            return this;
        }

        public Builder isSystem(boolean isSystem) {
            formingActivity.system = isSystem;
            return this;
        }

        public Builder type(ActivityType type) {
            formingActivity.action = type;
            return this;
        }

        public UserActivity build() {
            return formingActivity;
        }

    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }
}
