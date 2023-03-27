package com.breathingdetermination.socket;

import com.breathingdetermination.util.Connection;
import com.breathingdetermination.util.Coordinate;
import com.breathingdetermination.util.DataRecord;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
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

        for (String record : records) {
            coords = List.of(record.split(" +"));

            Coordinate chest = new Coordinate(Double.parseDouble(coords.get(1)),
                    Double.parseDouble(coords.get(2)), Double.parseDouble(coords.get(3)));
            Coordinate abdominal = new Coordinate(Double.parseDouble(coords.get(4)),
                    Double.parseDouble(coords.get(5)), Double.parseDouble(coords.get(6)));
            Coordinate back = new Coordinate(Double.parseDouble(coords.get(7)),
                    Double.parseDouble(coords.get(8)), Double.parseDouble(coords.get(9)));

            coordinates.add(new DataRecord(chest, abdominal, back, Double.parseDouble(coords.get(0))));
        }
        sendMessage();
    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println(throwable.getMessage());
        session.getBasicRemote();
    }

    private void sendMessage() throws IOException {
        try {
            String breathType = "-1";
            double end = coordinates.get(coordinates.size() - 1).getTimestamp();

            int startIndex = -1;
            for (int i = coordinates.size() - 1; i >= 0; i--) {
                if (end - coordinates.get(i).getTimestamp() >= ((float)60*166)/1500) {
                    startIndex = i;
                    break;
                }
            }

            if (startIndex != -1) {
                coordinates.removeAll(coordinates.subList(0, startIndex));
                String param = coordinates.toString().replace("[", "").replace("]", "").replace(", ", ",");
                ProcessBuilder processBuilder = new ProcessBuilder("python", "breath.py", param, modelType);
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();
                List<String> results = readProcessOutput(process.getInputStream());
                int res = process.exitValue();
                breathType = String.valueOf(res);
                System.out.println(results);
            }
            Connection.getInstance().getSession().getBasicRemote().sendText(breathType);
        } catch (IOException e) {
            System.out.println("Произошла ошибка при попытке вызова скрипта, ошибка:" + e.getMessage());
            sendErrorMessage("Произошла ошибка при попытке вызова скрипта, ошибка:" + e.getMessage());
        }
    }

    private List<String> readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader output = new BufferedReader(new InputStreamReader(inputStream))) {
            return output.lines()
                    .collect(Collectors.toList());
        }
    }

    private void sendErrorMessage(String message) throws IOException {
        Connection.getInstance().getSession().getBasicRemote().sendText(message);
    }
}
