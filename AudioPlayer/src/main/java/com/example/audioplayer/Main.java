package com.example.audioplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.*;

public class Main extends Application {

    private AudioPlayer player = new AudioPlayer();
    private MQTTClientHandler mqttClientHandler;

    @Override
    public void start(Stage stage) throws Exception {

        String broker = "tcp://10.42.0.1:1883";
        String clientId = MqttClient.generateClientId();
        mqttClientHandler = new MQTTClientHandler(broker, clientId);

        mqttClientHandler.subscribe("smartroom/audio", new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                System.out.println("Messaggio ricevuto sul topic " + topic + ": " + payload);

                switch (payload) {
                    case "TRIGGERED":
                        Platform.runLater(() -> triggered());
                        break;
                    case "VOLUME_50":
                        Platform.runLater(() -> volume(0.5));
                        break;
                    case "VOLUME_100":
                        Platform.runLater(() -> volume(1.0));
                        break;
                    case "VOLUME_0":
                        Platform.runLater(() -> volume(0.0));
                        break;

                    default:
                        System.out.println("Messaggio non riconosciuto");
                        break;
                }

            }

            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connessione persa con il broker MQTT");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Non ci interessa la consegna dei messaggi in questo caso
            }
        });

        player.getMediaPlayer().setOnEndOfMedia(() -> {
            Platform.runLater(() -> {
                try {
                    System.out.println("Audio terminato. Inviando messaggio di fine riproduzione.");
                    mqttClientHandler.publish("audio/status", "finito");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void triggered() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else {
            if (player.getStatus() == MediaPlayer.Status.STOPPED) {
                player.start("test.mp3");
            } else if (player.getStatus() == MediaPlayer.Status.PAUSED) {
                player.resume();
            }
        }
    }

    private void volume(double volume) {
        player.setVolume(volume);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
