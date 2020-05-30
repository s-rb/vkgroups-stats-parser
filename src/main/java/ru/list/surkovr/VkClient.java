package ru.list.surkovr;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VkClient extends VkApiClient {
    @Value("${client.secret}")
    private String clientSecret;
    @Value("${server.host}")
    private String host;
    @Value("${client.id}")
    private int clientId;
    private List<Integer> groupIds = List.of(88732008, 147604036, 158658142, 136695018, 1311599, 56631972);
    private String userId;

    public VkClient() {
        super(new HttpTransportClient());
    }

//    @PostConstruct
//    public void init() {
//        Properties properties = loadConfiguration();
//        Integer port = Integer.valueOf(properties.getProperty("server.port"));
//        String host = properties.getProperty("server.host");
//
//        Integer clientId = Integer.valueOf(properties.getProperty("client.id"));
//        String clientSecret = properties.getProperty("client.secret");
//
//        HandlerCollection handlers = new HandlerCollection();
//
//        ResourceHandler resourceHandler = new ResourceHandler();
////        resourceHandler.setDirectoriesListed(true);
//        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
//        resourceHandler.setResourceBase(VkClient.class.getResource("/static").getPath());
//
//        VkApiClient vk = new VkApiClient(new HttpTransportClient());
//        handlers.setHandlers(new Handler[]{resourceHandler, new RequestHandler(vk, clientId, clientSecret, host)});
//
//        Server server = new Server(port);
//        server.setHandler(handlers);
//
//        server.start();
//        server.join();
//    }
//
//    private Properties loadConfiguration() {
//        Properties properties = new Properties();
//        try (InputStream is = VkClient.class.getResourceAsStream("/config.properties")) {
//            properties.load(is);
//        } catch (IOException e) {
////            LOG.error("Can't load properties file", e);
//            throw new IllegalStateException(e);
//        }
//        return properties;
//    }


    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public List<Integer> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Integer> groupIds) {
        this.groupIds = groupIds;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
