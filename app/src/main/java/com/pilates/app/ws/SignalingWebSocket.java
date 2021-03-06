package com.pilates.app.ws;

import android.content.Context;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;
import com.pilates.app.UserRegistry;
import com.pilates.app.model.Action;
import com.pilates.app.model.ActionBody;
import com.pilates.app.model.ActionType;
import com.pilates.app.model.UserSession;
import com.pilates.app.util.Constant;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.content.Context.MODE_PRIVATE;

public class SignalingWebSocket {
    private static SignalingWebSocket instance = new SignalingWebSocket();
    public static String token;
    public static String classId;
    private final WebSocketFactory factory = new WebSocketFactory();
    private final SignalingWebSocketListener adapter = SignalingWebSocketListener.getInstance();

    private WebSocket ws;

    private SignalingWebSocket() {
        ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
        thread.scheduleAtFixedRate(() -> {
            System.out.println("HEYYYY IM THREAD WHICH WORKS EVERY 5 sec");
            if (Objects.nonNull(ws)) {
                WebSocketState state = ws.getState();
                System.out.println("WS STATE: " + state);

                if (adapter.isConnectionFailed()) {
                    recreateWs();
                }
            }

        }, 0, 30, TimeUnit.SECONDS);
    }

    private void recreateWs() {
        try {
            System.out.println("RECREATING WS");
            this.ws = ws.recreate();
            this.ws = ws.connect();
            UserSession user = UserRegistry.getInstance().getUser();
            if (Objects.nonNull(user) && user.isInit()) {
                final Long infoId = user.getUserId();
                final ActionBody body = ActionBody.newBuilder().withUSerId(infoId).withToken(token).withClassId(classId).build();
                sendMessage(new Action(ActionType.CONNECT, body));
            }
        } catch (Exception e) {
            System.out.println("Could not establish web socket connection: " + e.getMessage());
        }
    }

    public void recreate() {
        if(ws != null) {
            ws.disconnect();
            ws = null;
        }

        initWs();
    }

    public void stop() {
        if(ws != null) {
            ws.disconnect();
            ws = null;
        }
    }
    private void initWs() {
        try {
 //           ws = factory.createSocket("ws://18.203.172.206:8080/streaming/callone"); // aws signaling ip
//            ws = factory.createSocket("ws://192.168.33.31:8080/streaming/callone");  // local work ip
//            ws = factory.createSocket("ws://192.168.100.5:8080/streaming/callone");  // local ip home
//            ws = factory.createSocket("ws://192.168.99.1:8080/streaming/callone");  // local ip home
            ws = factory.createSocket(Constant.ServiceEndpoints.SIGNALING_APP);  // local ip Daniel home

            ws.addListener(adapter);
            ws = ws.connect();
        } catch (Exception e) {
            System.out.println("Could not establish web socket connection: " + e.getMessage());
        }
    }


    public static SignalingWebSocket getInstance() {
        if (instance == null) {
            instance = new SignalingWebSocket();
        }

        return instance;
    }

    public void sendMessage(final Action action) {
        System.out.println("SENDING MESSAGE: " + action.toString());
        if (adapter.isConnectionFailed()) {
            System.out.println("Could not send because connection failed");
            return;
        }
        ws.sendText(action.toString());
    }

}
