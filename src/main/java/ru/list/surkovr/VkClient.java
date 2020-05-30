package ru.list.surkovr;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VkClient extends VkApiClient {

    private List<Integer> groupIds = List.of(88732008, 147604036, 158658142, 136695018, 1311599, 56631972);

    public VkClient() {
        super(new HttpTransportClient());
    }

    public List<Integer> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Integer> groupIds) {
        this.groupIds = groupIds;
    }
}