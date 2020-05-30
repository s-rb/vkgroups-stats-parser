package ru.list.surkovr.model;

public class GroupsStatsResult extends GroupStats {

    private String period;

    public GroupsStatsResult(int groupId, String name, int allPostsCount, int allLikesCount,
                             int allViewsCount, int allMembersCount, String period, int allCommentsCount) {
        super(groupId, name, allPostsCount, allLikesCount, allViewsCount, allMembersCount, allCommentsCount);
        this.period = period;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
