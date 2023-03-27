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
import java.util.Objects;
import java.util.stream.Collectors;

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
        message = message.trim();
        List<String> records = List.of(message.split("\n"));
        if(records.isEmpty()) {
            System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
            sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
            return;
        }

        List<String> coords = List.of(records.get(0).split(" +"));
        if (coords.size() != 10) {
            System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
            sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
            return;
        }
        double previous;
        if(!coordinates.isEmpty()) {
            previous = coordinates.get(coordinates.size() -1).getTimestamp();
        } else {
            try {
                previous = Double.parseDouble(coords.get(0));
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                return;
            }
        }

        for (int i = 0; i < records.size(); i++) {
            coords = List.of(records.get(i).split(" +"));
            if (coords.size() != 10) {
                System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                return;
            }

            double current;
            try {
                current = Double.parseDouble(coords.get(0));
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                return;
            }

            if(previous >= current && !coordinates.isEmpty()) {
                sendErrorMessage("Время не может быть меньше или равно времени предыдушей записи.");
                return;
            }
            previous = current;

            for (String coordinate : coords) {
                try {
                    Double.parseDouble(coordinate);
                } catch (NumberFormatException e) {
                    System.out.println("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                    sendErrorMessage("Неверный формат данных. Данные должны содержать 10 чисел, разделенных пробелом, дробная часть от целой должна разделяться знаком точки.");
                    return;
                }
            }
        }
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
