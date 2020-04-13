package ninja.egg82.streamlabs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.*;
import com.github.scribejava.core.oauth.OAuth20Service;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import ninja.egg82.core.StreamlabsApi20;
import ninja.egg82.utils.FileUtil;

public class StreamlabsSocketClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OAuth20Service service;
    private final File cacheFile;

    private String webToken = null;
    private String socketToken = null;
    private Socket client = null;

    private Runnable onConnect = null;
    private Consumer<String> onDisconnect = null;
    private Runnable onReconnect = null;
    private Consumer<String> onEvent = null;

    public StreamlabsSocketClient(String clientID, String clientSecret, String callbackURL, File cacheFile) throws IOException, ExecutionException, InterruptedException {
        this.cacheFile = FileUtil.getOrCreateFile(cacheFile);
        this.service = new ServiceBuilder(clientID)
                .responseType("code")
                .apiSecret(clientSecret)
                .callback(callbackURL)
                .defaultScope("socket.token")
                .userAgent("egg82/StreamlabsSocketAPI")
                .build(StreamlabsApi20.instance());
        tryParseCache();
        if (webToken != null) {
            tryGetSocketToken();
        }
    }

    public boolean isAuthorized() { return webToken != null; }

    public boolean isConnected() { return client != null && client.connected(); }

    public String getAuthURL() { return service.getAuthorizationUrl(); }

    public void authorize(String authCode) throws IOException, ExecutionException, InterruptedException {
        OAuth2AccessToken accessToken = service.getAccessToken(authCode);
        webToken = accessToken.getAccessToken();

        writeCache();
        tryGetSocketToken();
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
            client.connect();
        }
    }

    public void authorizeWithAPIToken(String token) throws IOException, ExecutionException, InterruptedException {
        webToken = token;

        tryGetSocketToken();
        writeCache();
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
            client.connect();
        }
    }

    public void authorizeWithSocketToken(String token) throws IOException {
        socketToken = token;
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
            client.connect();
        }
    }

    public boolean reconnectSocket() throws IOException {
        if (socketToken != null) {
            if (client != null && client.connected()) {
                client.close();
            }
            tryGetSocket();
            client.connect();
            return true;
        }
        return false;
    }

    public boolean reauthorizeSocket() throws IOException, ExecutionException, InterruptedException {
        if (webToken != null) {
            tryGetSocketToken();
        }
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
            client.connect();
        }
        return socketToken != null;
    }

    public void close() {
        if (client != null && client.connected()) {
            client.close();
        }
    }

    public void connect() throws IOException, ExecutionException, InterruptedException {
        if (client != null && client.connected()) {
            client.close();
        }
        if (webToken != null && socketToken == null) {
            tryGetSocketToken();
        }
        tryGetSocket();
        client.connect();
    }

    public void onConnect(Runnable run) throws IOException {
        this.onConnect = run;
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
        }
    }

    public void onDisconnect(Consumer<String> consumer) throws IOException {
        this.onDisconnect = consumer;
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
        }
    }

    public void onReconnect(Runnable run) throws IOException {
        this.onReconnect = run;
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
        }
    }

    public void onEvent(Consumer<String> consumer) throws IOException {
        this.onEvent = consumer;
        if (client != null && client.connected()) {
            client.close();
            tryGetSocket();
        }
    }

    private void tryParseCache() throws IOException {
        try (
                FileReader reader = new FileReader(cacheFile);
                BufferedReader in = new BufferedReader(reader);
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    webToken = line;
                    return;
                }
            }
        }
    }

    private void writeCache() throws IOException {
        try (
                FileWriter writer = new FileWriter(cacheFile);
                BufferedWriter out = new BufferedWriter(writer)
        ) {
            out.write(webToken + System.lineSeparator());
        }
    }

    private void tryGetSocketToken() throws IOException, ExecutionException, InterruptedException {
        OAuthRequest request = new OAuthRequest(Verb.GET, "https://streamlabs.com/api/v1.0/socket/token");
        service.signRequest(webToken, request);
        request.addParameter(OAuthConstants.ACCESS_TOKEN, webToken);
        try (Response response = service.execute(request)) {
            if (response.getCode() == 200) {
                JsonNode body = OBJECT_MAPPER.readTree(response.getBody());
                String token = body.get("socket_token").textValue();
                if (token != null && !token.isEmpty()) {
                    socketToken = token;
                }
            } else {
                throw new IOException("Unexpected response code: " + response.getCode());
            }
        }
    }

    private void tryGetSocket() throws IOException {
        try {
            client = IO.socket("https://sockets.streamlabs.com?token=" + socketToken);
            client.on(Socket.EVENT_CONNECT, args -> { if (onConnect != null) { onConnect.run(); } });
            client.on(Socket.EVENT_DISCONNECT, args -> { if (onDisconnect != null) { onDisconnect.accept(Arrays.toString(args)); } });
            client.on(Socket.EVENT_RECONNECT, args -> { if (onConnect != null) { onReconnect.run(); } });
            client.on("event", args -> { if (onEvent != null) { onEvent.accept(Arrays.toString(args)); } });
        } catch (URISyntaxException ex) {
            // Should never happen
            throw new IOException("Illegal WebSocket URL identified.", ex);
        }
    }
}
