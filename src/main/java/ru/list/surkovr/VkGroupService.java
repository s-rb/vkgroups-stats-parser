package ru.list.surkovr;

import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.enums.WallFilter;
import com.vk.api.sdk.objects.groups.Fields;
import com.vk.api.sdk.objects.groups.GroupFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class VkGroupService {

    public static final int TIMEOUT_BEFORE_START_DB_UPDATE = 5000;
    public int DEFAULT_MAX_POSTS_COUNT = 100;
    public int DEFAULT_OFFSET = 0;
    @Value("${db.update.timeout}")
    public int updateTimeout;
    private final VkClient vk;
    private String host;
    private GroupStatsRepository groupStatsRepository;
    private AtomicBoolean isCodeValid = new AtomicBoolean(false);

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
                if (isCodeValid.get()) {
                try {
                    Thread.sleep(TIMEOUT_BEFORE_START_DB_UPDATE);
                    try {
                        loadStatsToDb();
                    } catch (NoSuchElementException e) {
                        e.printStackTrace();
                    }
                    Thread.sleep(updateTimeout);
                } catch (InterruptedException e) {
                    isCodeValid.set(false);
                    e.printStackTrace();
                } }
            }
        });
        dbUpdater.setDaemon(true);
        dbUpdater.start();
    }

    public void loadStatsToDb() throws NoSuchElementException {
        List<GroupStats> statsList = Optional.ofNullable(getWallStatFromVk()).orElseThrow();
        if (statsList.isEmpty()) isCodeValid.set(false);
        statsList.forEach(g -> groupStatsRepository.save(g));
    }

    public List<GroupStats> getWallStatFromVk() {
        if (vk.getCode() == null || vk.getCode().equals("")) return null;
        List<GroupStats> result = null;
        try {
            UserActor userActor = getUserAccessToken(vk.getClientId(), vk.getClientSecret(), vk.getCode());
            result = new LinkedList<>();
            List<GroupFull> groupsData = vk.groups().getById(userActor)
                    .groupIds(vk.getGroupIds().stream().map(String::valueOf)
                            .collect(Collectors.toList())).fields(Fields.MEMBERS_COUNT).execute();
            for (GroupFull group : groupsData) {
                GroupStats currentStats = calculateWallStat(userActor, group);
                result.add(currentStats);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isCodeValid.set(false);
        }
        return result;
    }

    private GroupStats calculateWallStat(UserActor userActor, GroupFull group) throws Exception {
        GetResponse stats = Objects.requireNonNull(getStatsResponseFromVk(
                userActor, group.getId(),null, null, null));
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
            GetResponse newStats = getStatsResponseFromVk(userActor, group.getId(), offset++, null, null);
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

    private GetResponse getStatsResponseFromVk(UserActor userActor, int owner_id, Integer offset,
                                               Integer maxPostsCount, WallFilter wallFilter) throws ClientException, ApiException {
        return vk.wall().get(userActor).ownerId(owner_id)
                .offset(Objects.requireNonNullElse(offset, DEFAULT_OFFSET))
                .count(Objects.requireNonNullElse(maxPostsCount, DEFAULT_MAX_POSTS_COUNT))
                .filter(Objects.requireNonNullElse(wallFilter, WallFilter.ALL))
                .extended(false).fields().execute();
    }

    private UserActor getUserAccessToken(int clientId, String clientSecret,
                                         String code) throws ClientException, ApiException {
        UserAuthResponse userAuthResponse = vk.oAuth().userAuthorizationCodeFlow(
                clientId, clientSecret, getUserRedirectUri(), code).execute();
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

    public boolean setCode(String code) {
        if (code != null && !code.equals("")) {
            vk.setCode(code);
            isCodeValid.set(true);
            return true;
        } else {
            return false;
        }
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
                + "&display=page&redirect_uri=" + getUserRedirectUri() + "&scope=groups&response_type=code";
    }

    public boolean hasValidCode() {
        return isCodeValid.get();
    }

//    private String getGroupOAuthUrl(int groupId) {
//        return "https://oauth.vk.com/authorize?client_id=" + vk.getClientId()
//                + "&display=page&redirect_uri=" + getGroupRedirectUri(groupId)
//                + "&scope=manage&response_type=code&group_ids=" + groupId;
//    }
}
