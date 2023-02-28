package com.breathingdetermination.util;

import jakarta.websocket.Session;

import java.net.Socket;

public class Connection {

    private static volatile Connection INSTANCE = null;

    private volatile Session session;

    private volatile Socket listener;

    private Connection() {

    }

    public static Connection getInstance() {
        Connection localInstance = INSTANCE;
        if (localInstance == null) {
            synchronized (Connection.class) {
                localInstance = INSTANCE;
                if (localInstance == null) {
                    INSTANCE = localInstance = new Connection();
                }
            }
        }
        return localInstance;
    }

    public Session getSession() {
        return session;
    }

    public Socket getListener() {
        return listener;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setListener(Socket listener) {
        this.listener = listener;
    }
}
