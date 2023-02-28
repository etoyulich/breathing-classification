package com.breathingdetermination.socket;

import com.breathingdetermination.util.DataRecord;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@ServerEndpoint(value = "/determine/{modelType}/{token}")
public class DeterminationSocket {

    private final String secretToken = "1A2b3f4xl9";
    private String modelType;

    private List<DataRecord> coordinates = new ArrayList<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "token") String token, @PathParam(value = "modelType") String modelType) throws IOException {

    }

    @OnMessage
    public void onMessage(String message) throws IOException {

    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable throwable) {

    }


}
