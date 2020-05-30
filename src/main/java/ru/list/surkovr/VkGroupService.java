package ru.list.surkovr;

import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ApiUserDeletedException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.exceptions.OAuthException;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.enums.WallFilter;
import com.vk.api.sdk.objects.groups.Fields;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class VkGroupService {

    public static final int TIMEOUT_BEFORE_START_DB_UPDATE = 5000;
    public int DEFAULT_MAX_POSTS_COUNT = 20;
    public int DEFAULT_OFFSET = 0;
    @Value("${db.update.timeout}")
    public int updateTimeout;
    private final VkClient vk;
    private final String host;
    private final GroupStatsRepository groupStatsRepository;
    private final AtomicBoolean isAutorized = new AtomicBoolean(false);
    private UserActor actor;

    @Autowired
    public VkGroupService(VkClient vk, GroupStatsRepository groupStatsRepository) {
        this.vk = vk;
        this.groupStatsRepository = groupStatsRepository;
        host = vk.getHost();
    }

    @PostConstruct
    public void start() {
        Thread dbUpdater = new Thread(() -> {
            while (true) {
                if (isAutorized.get()) {
                    try {
                        Thread.sleep(TIMEOUT_BEFORE_START_DB_UPDATE);
                        try {
                            loadStatsToDb();
                        } catch (NoSuchElementException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(updateTimeout);
                    } catch (InterruptedException e) {
                        isAutorized.set(false);
                        e.printStackTrace();
                    }
                }
            }
        });
        dbUpdater.setDaemon(true);
        dbUpdater.start();
    }

    public String authUser(String code) {
        System.out.println("===> authUser " + code);
        actor = getUserAccessToken(vk.getClientId(), vk.getClientSecret(), code);
        String res;
        if (actor != null) {
            System.out.println("===> authUser, actor: " + actor.getAccessToken() + " " + actor.getId());
            res = "redirect:/stats/today";
            isAutorized.set(true);
        } else {
            res = "redirect:/login";
        }
        return res;
    }

    public void loadStatsToDb() throws NoSuchElementException {
        List<GroupStats> statsList = Optional.ofNullable(getWallStatFromVk()).orElseThrow();
        if (statsList.isEmpty()) {
            isAutorized.set(false);
        }
        statsList.forEach(groupStatsRepository::save);
    }

    public List<GroupStats> getWallStatFromVk() {
        List<GroupStats> result = null;
        try {
            result = new LinkedList<>();
            List<GroupFull> groupsData = vk.groups().getById(actor)
                    .groupIds(vk.getGroupIds().stream().map(String::valueOf)
                            .collect(Collectors.toList())).fields(Fields.MEMBERS_COUNT).execute();
            for (GroupFull group : groupsData) {
                GroupStats currentStats = calculateWallStat(group);
                result.add(currentStats);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isAutorized.set(false);
        }
        return result;
    }

    private GroupStats calculateWallStat(GroupFull group) throws Exception {
        GetResponse stats = Objects.requireNonNull(getStatsResponseFromVk(
                group.getId(), null, null, null));
        int postsCount = stats.getCount();
        int viewsCount = (int) stats.getItems().stream()
                .collect(Collectors.summarizingInt(s -> s.getViews().getCount())).getSum();
        int likesCount = (int) stats.getItems().stream()
                .collect(Collectors.summarizingInt(s -> s.getLikes().getCount())).getSum();
        int commentsCount = (int) stats.getItems().stream()
                .collect(Collectors.summarizingInt(s -> s.getComments().getCount())).getSum();

        int tempCount = stats.getItems().size();
        int offset = 1;
        while (tempCount < postsCount) {
            GetResponse newStats = getStatsResponseFromVk(group.getId(), offset++, null, null);
            viewsCount += (int) newStats.getItems().stream()
                    .collect(Collectors.summarizingInt(s -> s.getViews().getCount())).getSum();
            likesCount += (int) newStats.getItems().stream()
                    .collect(Collectors.summarizingInt(s -> s.getLikes().getCount())).getSum();
            commentsCount += (int) newStats.getItems().stream()
                    .collect(Collectors.summarizingInt(s -> s.getComments().getCount())).getSum();
            tempCount += newStats.getItems().size();
        }
        return new GroupStats(group.getId(), group.getName(), postsCount,
                likesCount, viewsCount, group.getMembersCount(), commentsCount);
    }

    private GetResponse getStatsResponseFromVk(int owner_id, Integer offset,
                                               Integer maxPostsCount, WallFilter wallFilter) throws Exception {
        Thread.sleep(50);
        System.out.println("===> getStatsResponseFromVk userId " + actor.getId() + " token " + actor.getAccessToken());
        return vk.wall().get(actor).ownerId(-1 * owner_id)
                .count(Objects.requireNonNullElse(maxPostsCount, DEFAULT_MAX_POSTS_COUNT))
                .offset(Objects.requireNonNullElse(offset, DEFAULT_OFFSET))
                .filter(Objects.requireNonNullElse(wallFilter, WallFilter.ALL))
                .extended(false).fields().execute();
    }

    private UserActor getUserAccessToken(int clientId, String clientSecret,
                                         String code) {
        System.out.println("===> getUserAccessToken" + clientId + " " + clientSecret + " " + code);
        UserAuthResponse userAuthResponse;
        try {
            userAuthResponse = vk.oAuth().userAuthorizationCodeFlow(
                    clientId, clientSecret, getUserRedirectUri(), code).execute();
        } catch (OAuthException e) {
            e.getRedirectUri();
            return null;
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
            return null;
        }
        return new UserActor(userAuthResponse.getUserId(), userAuthResponse.getAccessToken());
    }

    // UnixTime int = new Date().getTime() / 1000L  | System.currentTimeMillis() / 1000L; | Instant.now().getEpochSecond();
//        GroupAuthResponse groupAuthResponse = vk.oAuth().groupAuthorizationCodeFlow(clientId, clientSecret, getGroupRedirectUri(groupId), code).execute();
    // access_token 1<<20
    // intervalCount > 0
    // filters = "яблоко,апельсин,банан"
    // MAX_POSTS_COUNT не более 100

    private String getUserRedirectUri() {
        return host;
    }

    private String getGroupRedirectUri(int groupId) {
        return host + "/group_callback?group_id=" + groupId;
    }

    public List<GroupsStatsResultDTO> getGroupStatByPeriod(String period) {
        List<GroupStats> currentStatsList = new LinkedList<>();
        List<GroupStats> lastStatsList = new LinkedList<>();
        for (int i : vk.getGroupIds()) {
            GroupStats currentStats = groupStatsRepository.findFirstByGroupIdAndUpdatedTimeIsLessThan(i, LocalDateTime.now());
            LocalDateTime startPeriod;
            GroupStats lastStats;
            switch (period.toLowerCase()) {
                case ("today"):
                    startPeriod = LocalDate.now().atStartOfDay();
                    break;
                case ("week"):
                    startPeriod = LocalDateTime.now().minusWeeks(1);
                    break;
                case ("month"):
                    startPeriod = LocalDateTime.now().minusMonths(1);
                    break;
                case ("year"):
                    startPeriod = LocalDateTime.now().minusYears(1);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + period.toLowerCase());
            }
            lastStats = groupStatsRepository.findFirstByGroupIdAndUpdatedTimeIsLessThan(i, startPeriod);
            if (currentStats != null && lastStats != null) {
                currentStatsList.add(currentStats);
                lastStatsList.add(lastStats);
            }
        }
        return calculateStatsDifference(currentStatsList, lastStatsList, period);
    }

    private List<GroupsStatsResultDTO> calculateStatsDifference(List<GroupStats> currentStatsList,
                                                                List<GroupStats> lastStatsList,
                                                                String period) {
        List<GroupsStatsResultDTO> res = new LinkedList<>();
        for (GroupStats groupStatsCur : currentStatsList) {
            int groupId = groupStatsCur.getGroupId();
            String groupName = groupStatsCur.getName();
            GroupStats groupStatsLast;
            try {
                groupStatsLast = lastStatsList.stream().filter(g -> g.getGroupId() == groupId).findFirst().orElseThrow();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            res.add(getGroupsStatsResultDTO(period, groupStatsCur, groupId, groupName, groupStatsLast));
        }
        return res;
    }

    private GroupsStatsResultDTO getGroupsStatsResultDTO(String period, GroupStats groupStatsCur,
                                                         int groupId, String groupName, GroupStats groupStatsLast) {
        int viewsCount = groupStatsCur.getAllViewsCount() - groupStatsLast.getAllViewsCount();
        int likesCount = groupStatsCur.getAllLikesCount() - groupStatsLast.getAllLikesCount();
        int postsCount = groupStatsCur.getAllPostsCount() - groupStatsLast.getAllPostsCount();
        int commentsCount = groupStatsCur.getAllCommentsCount() - groupStatsLast.getAllCommentsCount();
        int membersCount = groupStatsCur.getAllMembersCount() - groupStatsLast.getAllMembersCount();
        return new GroupsStatsResultDTO(groupId, groupName, postsCount,
                likesCount, viewsCount, membersCount, period, commentsCount);
    }

    public String getUserOAuthUrl() {
        return "https://oauth.vk.com/authorize?client_id=" + vk.getClientId()
                + "&display=page&redirect_uri=" + getUserRedirectUri() + "&scope=stats&response_type=code";
    }

    public boolean hasValidAccessToken() {
        return isAutorized.get();
    }

//    private String getGroupOAuthUrl(int groupId) {
//        return "https://oauth.vk.com/authorize?client_id=" + vk.getClientId()
//                + "&display=page&redirect_uri=" + getGroupRedirectUri(groupId)
//                + "&scope=manage&response_type=code&group_ids=" + groupId;
//    }
}
