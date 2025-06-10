package com.smartroom;

import com.smartroom.server.WebServer;
import com.smartroom.server.MqttService;

import io.vertx.core.Vertx;

public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // WebServer.start(vertx); // Avvia HTTP
        new MqttService(vertx, "localhost", 1883); // Avvia MQTT
    }
}
