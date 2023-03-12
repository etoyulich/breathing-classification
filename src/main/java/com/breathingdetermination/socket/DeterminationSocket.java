package com.breathingdetermination.socket;

import com.breathingdetermination.util.Connection;
import com.breathingdetermination.util.Coordinate;
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
        if (!Objects.equals(secretToken, token)) {
            System.out.println("Введен неверный секретный токен.");
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Введен неверный секретный токен."));
            return;
        }
        if (!Objects.equals(modelType, "1") && !Objects.equals(modelType, "0")) {
            System.out.println("Введен неправильный параметр типа используемой модели.");
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Введен неправильный параметр типа используемой модели."));
            return;
        }
        if (Connection.getInstance().getSession() != null) {
            System.out.println("Невозможно подключиться к серверу. Уже существует открытое подключение.");
            session.close(new CloseReason(CloseReason.CloseCodes.TRY_AGAIN_LATER, "Невозможно подключиться к серверу. Уже существует открытое подключение."));
            return;
        }
        Connection.getInstance().setSession(session);
        Connection.getInstance().setListener(new Socket());
        this.modelType = modelType;
        System.out.println("New session connected!");
    }

    @OnMessage
    public void onMessage(String message) throws IOException {

    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println(throwable.getMessage());
        session.getBasicRemote();
    }


}
