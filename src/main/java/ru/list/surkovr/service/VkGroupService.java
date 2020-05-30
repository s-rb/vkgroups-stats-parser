package ru.list.surkovr.service;

import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
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
import ru.list.surkovr.VkClient;
import ru.list.surkovr.model.GroupStats;
import ru.list.surkovr.model.GroupsStatsResult;
import ru.list.surkovr.repository.GroupStatsRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class VkGroupService {
    @Value("${timeout.before_start_db_update}")
    public int timeoutBeforeStartDbUpdate;
    @Value("${timeout.to_get_data_from_vk_ms}")
    public int timeoutToGetFromVkMs;
    @Value("${timeout.clear_db_ms}")
    public int cleardbTimeoutMs;
    @Value("${default.max_posts_count}")
    public int defaultMaxPostsCount;
    @Value("${default.offset}")
    public int defaultOffset;
    @Value("${timeout.db_update}")
    public int updateTimeout;
    @Value("${server.host}")
    public String host;
    @Value("${client.secret}")
    private String clientSecret;
    @Value("${client.id}")
    private int clientId;

    private final VkClient vk;
    private final GroupStatsRepository groupStatsRepository;
    private final AtomicBoolean isAutorized = new AtomicBoolean(false);
    private UserActor actor;

    @Autowired
    public VkGroupService(VkClient vk, GroupStatsRepository groupStatsRepository) {
        this.vk = vk;
        this.groupStatsRepository = groupStatsRepository;
    }

    @PostConstruct
    public void startDbUpdater() {
        Thread dbUpdater = new Thread(() -> {
            while (true) {
                if (isAutorized.get()) {
                    try {
                        Thread.sleep(timeoutBeforeStartDbUpdate);
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
        actor = getUserAccessToken(clientId, clientSecret, code);
        String res;
        if (actor != null) {
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
        statsList.forEach(o -> {
            groupStatsRepository.save(o);
            try {
                Thread.sleep(cleardbTimeoutMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
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
        if (stats.getItems().isEmpty()) throw new NullPointerException("stats.getItems returns null");
        int postsCount = stats.getCount();
        int viewsCount = stats.getItems().stream().map(
                s -> s.getViews().getCount()).reduce(Integer::sum).orElse(0);
        int likesCount = stats.getItems().stream().map(
                s -> s.getLikes().getCount()).reduce(Integer::sum).orElse(0);
        int commentsCount = stats.getItems().stream().map(
                s -> s.getComments().getCount()).reduce(Integer::sum).orElse(0);
        int tempCount = stats.getItems().size();
        int offset = 1;
        while (tempCount < postsCount) {
            GetResponse newStats = getStatsResponseFromVk(group.getId(), offset++, null, null);
            viewsCount += newStats.getItems().stream().map(
                    s -> s.getViews().getCount()).reduce(Integer::sum).orElse(0);
            likesCount += newStats.getItems().stream().map(
                    s -> s.getLikes().getCount()).reduce(Integer::sum).orElse(0);
            commentsCount += newStats.getItems().stream().map(
                    s -> s.getComments().getCount()).reduce(Integer::sum).orElse(0);
            tempCount += newStats.getItems().size();
        }
        return new GroupStats(group.getId(), group.getName(), postsCount,
                likesCount, viewsCount, group.getMembersCount(), commentsCount);
    }

    private GetResponse getStatsResponseFromVk(int ownerId, Integer offset,
                                               Integer maxPostsCount, WallFilter wallFilter) throws Exception {
        Thread.sleep(timeoutToGetFromVkMs);
        return vk.wall().get(actor).ownerId(-1 * ownerId)
                .count(Objects.requireNonNullElse(maxPostsCount, defaultMaxPostsCount))
                .offset(Objects.requireNonNullElse(offset, defaultOffset))
                .filter(Objects.requireNonNullElse(wallFilter, WallFilter.ALL))
                .extended(false).fields().execute();
    }

    private UserActor getUserAccessToken(int clientId, String clientSecret,
                                         String code) {
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

    private String getUserRedirectUri() {
        return host;
    }

    public List<GroupsStatsResult> getGroupStatByPeriod(String period) {
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

    private List<GroupsStatsResult> calculateStatsDifference(List<GroupStats> currentStatsList,
                                                             List<GroupStats> lastStatsList,
                                                             String period) {
        List<GroupsStatsResult> res = new LinkedList<>();
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

    private GroupsStatsResult getGroupsStatsResultDTO(String period, GroupStats groupStatsCur,
                                                      int groupId, String groupName, GroupStats groupStatsLast) {
        int viewsCount = groupStatsCur.getAllViewsCount() - groupStatsLast.getAllViewsCount();
        int likesCount = groupStatsCur.getAllLikesCount() - groupStatsLast.getAllLikesCount();
        int postsCount = groupStatsCur.getAllPostsCount() - groupStatsLast.getAllPostsCount();
        int commentsCount = groupStatsCur.getAllCommentsCount() - groupStatsLast.getAllCommentsCount();
        int membersCount = groupStatsCur.getAllMembersCount() - groupStatsLast.getAllMembersCount();
        return new GroupsStatsResult(groupId, groupName, postsCount,
                likesCount, viewsCount, membersCount, period, commentsCount);
    }

    public String getUserOAuthUrl() {
        return "https://oauth.vk.com/authorize?client_id=" + clientId
                + "&display=page&redirect_uri=" + getUserRedirectUri() + "&scope=stats&response_type=code";
    }

    public boolean hasValidAccessToken() {
        return isAutorized.get();
    }
}
