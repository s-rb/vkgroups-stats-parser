package ru.list.surkovr.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_stats")
public class GroupStats implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "group_id")
    private int groupId;
    @Column(unique = true)
    private String name;
    @Column(name = "all_posts_count")
    private int allPostsCount;
    @Column(name = "all_likes_count")
    private int allLikesCount;
    @Column(name = "all_views_count")
    private int allViewsCount;
    @Column(name = "all_members_count")
    private int allMembersCount;
    @Column(name = "all_comments_count")
    private int allCommentsCount;
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    public GroupStats(int groupId, String name, int allPostsCount, int allLikesCount, int allViewsCount,
                      int allMembersCount, int allCommentsCount) {
        this.groupId = groupId;
        this.name = name;
        this.allPostsCount = allPostsCount;
        this.allLikesCount = allLikesCount;
        this.allViewsCount = allViewsCount;
        this.allMembersCount = allMembersCount;
        this.allCommentsCount = allCommentsCount;
        updatedTime = LocalDateTime.now();
    }

    public GroupStats() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAllPostsCount() {
        return allPostsCount;
    }

    public void setAllPostsCount(int allPostsCount) {
        this.allPostsCount = allPostsCount;
    }

    public int getAllLikesCount() {
        return allLikesCount;
    }

    public void setAllLikesCount(int allLikesCount) {
        this.allLikesCount = allLikesCount;
    }

    public int getAllViewsCount() {
        return allViewsCount;
    }

    public void setAllViewsCount(int allViewsCount) {
        this.allViewsCount = allViewsCount;
    }

    public int getAllMembersCount() {
        return allMembersCount;
    }

    public void setAllMembersCount(int allMembersCount) {
        this.allMembersCount = allMembersCount;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public int getAllCommentsCount() {
        return allCommentsCount;
    }

    public void setAllCommentsCount(int allCommentsCount) {
        this.allCommentsCount = allCommentsCount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
