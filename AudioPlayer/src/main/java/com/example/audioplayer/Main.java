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
    private final String deviceId = "audioPlayer";

    private final String brokerHost = "192.168.0.2";
    private final int brokerPort = 1883;

    private final String commandTopic = "bonci/audioPlayer/command"; // broker -> audioPlayer
    private final String dataTopic = "bonci/online_data"; // audioPlayer -> broker

    @Override
    public void start(Stage stage) {
        vertx = Vertx.vertx();
        connectToBroker();
    }

    private void connectToBroker() {

        JsonObject lwt = new JsonObject()
            .put("online", false)
            .put("deviceId", deviceId)
            .put("os", System.getProperty("os.name"))
            .put("timestamp", System.currentTimeMillis());
        
        MqttClientOptions options = new MqttClientOptions()
            .setAutoKeepAlive(true)
            .setWillTopic(dataTopic)
            .setWillMessage(Buffer.buffer(lwt.encode()))
            .setWillQos(MqttQoS.AT_LEAST_ONCE);

        client = MqttClient.create(vertx, options);
        attemptConnection();
    }

    private void attemptConnection() {
        System.out.println("Tentativo di connessione (60 sec max)");
        client.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("Connesso al broker");

                client.subscribe(commandTopic, 1);

                JsonObject onlinePayload = new JsonObject()
                        .put("online", true)
                        .put("deviceId", deviceId)
                        .put("os", System.getProperty("os.name"))
                        .put("timestamp", System.currentTimeMillis());

                client.publish(
                        dataTopic,
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
            case "ON":
                on();
                break;
            case "OFF":
                off();
                break;
            case "PAUSE":
                pause();
                break;
            case "SHUTDOWN":
                shutdown();
            default:
                System.out.println("Messaggio non riconosciuto: " + payload);
        }
    }

    private void on() {
        if (player.getStatus() == MediaPlayer.Status.STOPPED) {
            player.start("test.mp3");
        } else if (player.getStatus() == MediaPlayer.Status.PAUSED) {
            player.resume();
        }
    }

    private void off() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.stop();
        }
    }

    private void pause() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        }
    }

    private void volume(double volume) {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.setVolume(volume);
        }
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

    public static void shutdown() {
        try {
            Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}