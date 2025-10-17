package dev.fusionize.user.activity;

import java.util.ArrayList;
import java.util.List;

public class AuditableEntity {
    private List<UserActivity> userActivities =  new ArrayList<>();

    public void pushActivity(UserActivity activity){
        this.userActivities.add(activity);
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "userActivities=" + userActivities +
                '}';
    }

    public List<UserActivity> getActivitiesBy(String user, ActivityType activityType){
        return userActivities.stream()
                .filter(a-> user == null || user.equals(a.getUser()))
                .filter(a-> activityType == null || activityType.equals(a.getAction())).toList();
    }


    public List<UserActivity> getUserActivities() {
        return userActivities;
    }

    public void setUserActivities(List<UserActivity> userActivities) {
        this.userActivities = userActivities;
    }
}
