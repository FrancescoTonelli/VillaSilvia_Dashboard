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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Main extends Application {

    private final String wifiSSID = "Bonci_WiFi";      
    private final String wifiPassword = "BonciRoom1";

    private Vertx vertx;
    private MqttClient client;
    private AudioPlayer player = new AudioPlayer();

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
                .put("deviceId", "audioPlayer")
                .put("os", System.getProperty("os.name"))
                .put("timestamp", System.currentTimeMillis());

        MqttClientOptions options = new MqttClientOptions()
                .setAutoKeepAlive(true)
                .setWillFlag(true)
                .setWillTopic(dataTopic)
                .setWillMessage(lwt.encode())
                .setWillQoS(0)
                .setWillRetain(false);

        client = MqttClient.create(vertx, options);
        attemptConnection();
    }

    private boolean checkWifi() {
        try {
            Process check = Runtime.getRuntime().exec("iwgetid -r");
            check.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(check.getInputStream()));
            String ssid = reader.readLine();

            if (ssid != null && !ssid.isEmpty()) {
                System.out.println("Wi-Fi connesso alla rete: " + ssid);
                return true;
            }
            return false;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Assicura di essersi connesso al WiFi
    private boolean ensureWifiConnected() {
        try {

            if (checkWifi()) {
                return true;
            }

            Process connect = Runtime.getRuntime().exec(
                    new String[]{"bash", "-c", "nmcli dev wifi connect '" + wifiSSID + "' password '" + wifiPassword + "'"});
            connect.waitFor();

    
            if (checkWifi()) {
                return true;
            } else {
                System.err.println("Impossibile riconnettersi al Wi-Fi.");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void attemptConnection() {

        if (!ensureWifiConnected()) {
            System.err.println("Nessuna connessione Wi-Fi. Riprovo tra 10s...");
            vertx.setTimer(10_000, id -> attemptConnection());
            return;
        }

        client.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("Connesso al broker");

                client.subscribe(commandTopic, 1);

                JsonObject onlinePayload = new JsonObject()
                        .put("online", true)
                        .put("deviceId", "audioPlayer")
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
            case "VOLUME_UP":
                volume_up();
                break;
            case "VOLUME_DOWN":
                volume_down();
                break;
            case "SHUTDOWN":
                shutdown();
                break;
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

    private void volume_up() {
        double currentVolume = player.getVolume();
        if (currentVolume < 1.0) {
            player.setVolume(currentVolume + 0.1);
        }

    }

    private void volume_down() {
        double currentVolume = player.getVolume();
        if (currentVolume > 0.1) {
            player.setVolume(currentVolume - 0.1);
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