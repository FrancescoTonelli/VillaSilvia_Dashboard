package com.smartroom.server;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import com.smartroom.model.DeviceStatusManager;

public class MqttService {

    private final MqttClient client;
    private final String audioTopic = "bonci/audioPlayer/command"; // broker -> audioPlayer (ON,OFF,VOLUME...)
    private final String plafTopic = "bonci/plafoniere/command"; // broker -> plafoniere (ON,OFF,LIGHT_UP......)
    private final String videoTopic = "bonci/videoPlayer/command"; // broker -> videoPlayer
    private final String videoEventTopic = "bonci/videoPlayer/event"; // videoPlayer -> broker (TRIGGERED,ENDED)
    private final String powerTopic = "bonci/power/command"; // pannello -> broker (SHUTDOWN,SLEEP,WAKE_UP)
    private final String dataTopic = "bonci/online_data"; // dispositivi -> broker (DEVICEID,IP......)

    private Boolean pianoAlreadyTriggered = false;
    // si riferisce alla stazione del pianoforte, infatti il suo sonar
    // può essere triggerato 2 volte

    public MqttService(Vertx vertx, String brokerHost, int brokerPort) {
        this.client = MqttClient.create(vertx, new MqttClientOptions());

        client.connect(brokerPort, brokerHost, result -> {
            if (result.succeeded()) {
                System.out.println("MQTT connesso a " + brokerHost + ":" + brokerPort);
                subscribeToTopics();
                setupMessageHandler(vertx);
            } else {
                System.err.println("Connessione MQTT fallita: " + result.cause().getMessage());
            }
        });
    }

    // Iscrizione ai topic MQTT da cui deve ricevere messaggi
    private void subscribeToTopics() {
        client.subscribe(dataTopic, 0);
        client.subscribe(videoEventTopic, 0);
        client.subscribe(powerTopic, 0);
    }

    // Gestione dei messaggi MQTT ricevuti
    private void setupMessageHandler(Vertx vertx) {
        client.publishHandler(message -> {
            String topic = message.topicName();
            String payload = message.payload().toString("UTF-8");
            String deviceId;

            JsonObject data;
            try {
                data = new JsonObject(payload);
            } catch (Exception e) {
                System.err.println("Payload non valido (non JSON) su topic " + topic + ": " + payload);
                return;
            }

            switch (topic) {
                // topic per ricevere i comandi di spegnimento, sleep e wake
                case powerTopic:
                    String command = data.getString("command");
                    if (command == null) {
                        System.err.println("Comando assente in JSON su topic 'control': " + payload);
                        return;
                    }
                    handleControl(command);
                    break;
                // topic per ricevere gli eventi del videoplayer sulla riproduzione video
                // (triggered,ended)
                case videoEventTopic:
                    deviceId = data.getString("deviceId");
                    String event = data.getString("event");
                    if (deviceId == null) {
                        System.err.println("deviceId mancante nel messaggio JSON su topic: " + topic);
                        return;
                    }

                    if (event.equals("triggered")) {
                        handleTriggered(deviceId, data, vertx);
                    } else if (event.equals("ended")) {
                        handleEnded(deviceId);
                    }
                    break;
                // topic per ricevere lo stato dei device appena si connettono alla rete (si
                // distinguono per deviceId)
                case dataTopic:
                    deviceId = data.getString("deviceId");
                    if (deviceId == null) {
                        System.err.println("deviceId mancante nel messaggio JSON su topic: " + topic);
                        return;
                    }
                    handleData(deviceId, data);
                    break;

            }

        });
    }

    // Pubblica un messaggio su un topic
    public void publish(String topic, String message) {
        if (client.isConnected()) {
            client.publish(topic, Buffer.buffer(message), MqttQoS.AT_LEAST_ONCE, false, false);
            System.out.println("Pubblicato su " + topic + ": " + message);
        } else {
            System.err.println("Client MQTT non connesso!");
        }
    }

    // Gestione Shelly con timer
    public void shellyManager(JsonArray lights, Vertx vertx) {
        lights.forEach(entry -> {
            JsonObject light = (JsonObject) entry;
            String id = light.getString("id");
            int onAfter = light.getInteger("onAfter", 0);
            int offAfter = light.getInteger("offAfter", 0);
            String topic = id + "/rpc";

            if (onAfter > 0) {
                vertx.setTimer(onAfter * 1000L, t -> publishShellyCommand(topic, true));
            }
            if (offAfter > 0) {
                vertx.setTimer(offAfter * 1000L, t -> publishShellyCommand(topic, false));
            }
        });
    }

    private void publishShellyCommand(String topic, boolean on) {
        JsonObject command = new JsonObject()
                .put("id", 1)
                .put("src", "server")
                .put("method", "Switch.Set")
                .put("params", new JsonObject().put("id", 0).put("on", on));

        publish(topic, command.encode());
        System.out.println("Luce Shelly " + (on ? "accesa" : "spenta") + " su topic: " + topic);
    }

    // Handlers dei vari eventi

    private void handleTriggered(String deviceId, JsonObject data, Vertx vertx) {
        System.out.println("Trigger ricevuto da " + deviceId + ": " + data.encodePrettily());
        JsonArray lights = data.getJsonArray("lights");

        if (deviceId.equals("videoPlayer-intro")) {
            publish(plafTopic, "LIGHT_DOWN");
            publish(audioTopic, "OFF");
        }

        if (deviceId.equals("videoPlayer-piano") && pianoAlreadyTriggered) {
            pianoAlreadyTriggered = false;
            lights.forEach(entry -> {
                JsonObject light = (JsonObject) entry;
                String id = light.getString("id");
                String topic = id + "/rpc";

                publishShellyCommand(topic, false);
            });
            publish(plafTopic, "OFF");
            return;
        }
        if (deviceId.equals("videoPlayer-piano")) {
            pianoAlreadyTriggered = true;
        }

        if (lights != null) {
            shellyManager(lights, vertx);
        }
    }

    private void handleEnded(String deviceId) {
        System.out.println("Video terminato su " + deviceId);
        if (deviceId.equals("videoPlayer-piano")) {
            publish(plafTopic, "ON");
            publish(plafTopic, "LIGHT_UP");
            publish(audioTopic, "ON");
        }
    }

    private void handleData(String deviceId, JsonObject data) {
        if (deviceId != null) {
            DeviceStatusManager.updateDeviceStatus(deviceId, data);
            System.out.println("Stato aggiornato per: " + deviceId);
        }

        if (deviceId.contains("audioPlayer")) {
            System.out.println("Audio player connesso");
            publish(audioTopic, "ON");
        }

        if (deviceId.contains("plafoniera")) {
            System.out.println("Plafoniera connessa");
            publish("bonci/" + deviceId + "/command", "STARTING");
            // lo manda solo alla plafoniera specifica non a tutte
        }

        if (deviceId.contains("videoPlayer")) {
            System.out.println("Video player connesso");
        }
    }

    public void handleControl(String command) {
        switch (command) {
            case "shutdown":
                publish(audioTopic, "SHUTDOWN");
                publish(videoTopic, "SHUTDOWN");
                try {
                    Runtime.getRuntime().exec("sudo shutdown -h now");
                    System.out.println("Spegnimento Raspberry Pi in corso...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "sleep":
                publish(audioTopic, "OFF");
                publish(plafTopic, "OFF");
                publish(videoTopic, "SLEEP");
                break;
            case "wake":
                publish(audioTopic, "ON");
                publish(plafTopic, "ON");
                publish(videoTopic, "WAKE");
                break;
            case "start_presentation":
                publish(plafTopic, "STARTING");
                publish(audioTopic, "ON");

                DeviceStatusManager.getAllDevices().keySet().stream()
                        .filter(dev -> dev.contains("shelly"))
                        .forEach(dev -> handleDeviceCommand(dev, "OFF"));
                System.out.println("Presentazione avviata: luce generale accesa, audio acceso");
                break;
            default:
                System.err.println("Comando sconosciuto su topic 'control': " + command);
        }
    }

    public void handleGeneralLight(String command) {
        if (command == null) {
            System.err.println("Comando mancante per luce generale");
            return;
        }

        switch (command) {
            case "ON":
                publish(plafTopic, "ON");
                System.out.println("Luce generale accesa");
                break;
            case "OFF":
                publish(plafTopic, "OFF");
                System.out.println("Luce generale spenta");
                break;
            case "LIGHT_UP":
                publish(plafTopic, "LIGHT_UP");
                System.out.println("Luce generale in aumento");
                break;
            case "LIGHT_DOWN":
                publish(plafTopic, "LIGHT_DOWN");
                System.out.println("Luce generale in diminuzione");
                break;
            case "WARM_UP":
                publish(plafTopic, "WARM_UP");
                System.out.println("Luce generale calda");
                break;
            case "COLD_UP":
                publish(plafTopic, "COLD_UP");
                System.out.println("Luce generale fredda");
                break;
            default:
                System.err.println("Comando sconosciuto per luce generale: " + command);
        }
    }

    public void handleGeneralAudio(String command) {
        if (command == null) {
            System.err.println("Comando mancante per audio");
            return;
        }

        switch (command) {
            case "ON":
                publish(audioTopic, "ON");
                System.out.println("Audio acceso");
                break;
            case "OFF":
                publish(audioTopic, "OFF");
                System.out.println("Audio spento");
                break;
            case "AUDIO_UP":
                publish(audioTopic, "AUDIO_UP");
                System.out.println("Audio in aumento");
                break;
            case "AUDIO_DOWN":
                publish(audioTopic, "AUDIO_DOWN");
                System.out.println("Audio in diminuzione");
                break;
            default:
                System.err.println("Comando sconosciuto per audio: " + command);
        }
    }

    public void handleGeneralVideo(String command) {
        if (command == null) {
            System.err.println("Comando mancante per video");
            return;
        }

        switch (command) {
            case "SLEEP":
                publish(videoTopic, "SLEEP");
                System.out.println("Video spento");
                break;
            case "WAKE":
                publish(videoTopic, "WAKE");
                System.out.println("Video acceso");
                break;
            default:
                System.err.println("Comando sconosciuto per video: " + command);
        }
    }

    public void handleDeviceCommand(String deviceId, String command) {
        if (deviceId == null || command == null) {
            System.err.println("ID dispositivo o comando mancante");
            return;
        }

        if (deviceId.contains("plafoniera")) {
            publish("bonci/" + deviceId + "/command", command);
            System.out.println("Comando " + command + " inviato a " + deviceId + ".");
        }

        else if (deviceId.contains("shelly") && (command.equals("ON") || command.equals("OFF"))) {
            publishShellyCommand(deviceId + "/rpc", command.equals("ON") ? true : false);
        }
    }
}
