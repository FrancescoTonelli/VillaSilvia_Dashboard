package com.example.audioplayer;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import io.vertx.core.buffer.Buffer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Main extends Application {

    private Vertx vertx;
    private MqttClient client;
    private AudioPlayer player = new AudioPlayer();

    private final String brokerHost = "10.42.0.1";
    private final int brokerPort = 1883;

    @Override
    public void start(Stage stage) {
        vertx = Vertx.vertx();
        connectToBroker();
    }

    private void connectToBroker() {
        client = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));

        attemptConnection();
    }

    private void attemptConnection() {
        System.out.println("Tentativo di connessione (60 sec max)");
        client.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("Connesso al broker");

                client.subscribe("smartroom/audio/volume", 1);

                JsonObject onlinePayload = new JsonObject()
                        .put("online", true)
                        .put("deviceId", "audioPlayer")
                        // .put("deviceName", deviceName)
                        // .put("ipAddress", getLocalIpAddress())
                        // .put("uptime", System.currentTimeMillis() - appStartTime)
                        .put("freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024))
                        .put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024))
                        .put("os", System.getProperty("os.name"))
                        .put("timestamp", System.currentTimeMillis());

                client.publish(
                        "smartroom/audio/data",
                        Buffer.buffer(onlinePayload.encode()),
                        MqttQoS.AT_LEAST_ONCE,
                        false,
                        false);

                client.publishHandler(message -> {
                    String topic = message.topicName();
                    String payload = message.payload().toString();

                    System.out.println("Messaggio ricevuto: " + topic + " → " + payload);

                    Platform.runLater(() -> handleMessage(payload));
                });

                client.closeHandler(v -> {
                    System.err.println("Connessione MQTT persa. Riprovo tra 10s...");
                    vertx.setTimer(10_000, id -> attemptConnection());
                    Platform.runLater(() -> player.stop());
                });

            } else {
                System.err.println("Dopo 60 secondi il tentativo di connessione è fallito");
                attemptConnection();
            }
        });
    }

    private void handleMessage(String payload) {
        switch (payload) {
            case "TRIGGERED":
                triggered();
                break;
            case "50":
                volume(0.5);
                break;
            case "100":
                volume(1.0);
                break;
            case "0":
                volume(0.0);
                break;
            default:
                System.out.println("Messaggio non riconosciuto: " + payload);
        }
    }

    private void triggered() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else if (player.getStatus() == MediaPlayer.Status.STOPPED) {
            player.start("test.mp3");
        } else if (player.getStatus() == MediaPlayer.Status.PAUSED) {
            player.resume();
        }
    }

    private void volume(double volume) {
        player.setVolume(volume);
    }

    @Override
    public void stop() {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
        if (vertx != null) {
            vertx.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}