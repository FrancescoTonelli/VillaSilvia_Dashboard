package com.smartroom.server;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import com.smartroom.model.DeviceStatusManager;

public class MqttService {

    private final Vertx vertx;
    private final String brokerHost;
    private final int brokerPort;
    private final MqttClient client;
    private final LogAgent logger;

    private static final String WIFI_SSID = "Bonci_WiFi";
    private static final String WIFI_PASSWORD = "BonciRoom1";

    private final String audioTopic = "bonci/audioPlayer/command"; // broker -> audioPlayer (ON,OFF,VOLUME...)
    private final String plafTopic = "bonci/plafoniere/command"; // broker -> plafoniere (ON,OFF,LIGHT_UP......)
    private final String videoTopic = "bonci/videoPlayer/command"; // broker -> videoPlayer
    private final String videoEventTopic = "bonci/videoPlayer/event"; // videoPlayer -> broker (TRIGGERED,ENDED)
    private final String powerTopic = "bonci/power/command"; // pannello -> broker (SHUTDOWN,SLEEP,WAKE_UP)
    private final String dataTopic = "bonci/online_data"; // dispositivi -> broker (DEVICEID,IP......)
    private final String logTopic = "bonci/log"; // dispositivi -> broker (messaggi di log)

    private final List<String> lightCommands = List.of("ON", "OFF", "LIGHT_UP", "LIGHT_DOWN", "COLD_UP", "WARM_UP");
    private final List<String> audioCommands = List.of("ON", "OFF", "AUDIO_UP", "AUDIO_DOWN", "SHUTDOWN");
    private final List<String> videoCommands = List.of("SLEEP", "WAKE");

    private Boolean pianoAlreadyTriggered = false;
    private Boolean introAlreadyTriggered = false;

    private long lastWakeTime = 0;
    private final long WAKE_COOLDOWN = 10000;

    public MqttService(Vertx vertx, String brokerHost, int brokerPort) {
        this.vertx = vertx;
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.client = MqttClient.create(vertx, new MqttClientOptions());
        this.logger = new LogAgent(vertx);

        attemptConnection();

        // Failsafe periodico: se MQTT si disconnette o il Wi-Fi cade
        vertx.setPeriodic(10000, id -> {
            if (!client.isConnected() || !isWifiConnected()) {
                log("ERROR", "broker", "Verifica connettività fallita: riavvio connessione...");
                attemptConnection();
            }
        });
    }

    public void log(String type, String device, String message) {
        this.logger.log(type, device, message);
        var stream = type.equals("ERROR") ? System.err : System.out;
        stream.println(device + ": " + type + " | " + message);
    }

    private void attemptConnection() {
        if (!ensureWifiConnected()) {
            log("ERROR", "broker", "Wi-Fi non connesso. Riprovo tra 10s...");
            vertx.setTimer(10000, id -> attemptConnection());
            return;
        }

        log("INFO", "broker", "Wi-Fi OK, connetto a MQTT...");
        if (!client.isConnected()) {
            client.connect(brokerPort, brokerHost, res -> {
                if (res.succeeded()) {
                    log("INFO", "broker", "MQTT connesso a " + brokerHost + ":" + brokerPort);
                    subscribeToTopics();
                    setupMessageHandler();
                    client.closeHandler(v -> {
                        log("ERROR", "broker", "Connessione MQTT persa");
                        attemptConnection();
                    });
                } else {
                    log("ERROR", "broker", "Connessione MQTT fallita: " + res.cause().getMessage());
                    vertx.setTimer(5000, tid -> attemptConnection());
                }
            });
        }
    }

    private boolean isWifiConnected() {
        try {
            Process check = Runtime.getRuntime().exec("iwgetid -r");
            check.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(check.getInputStream()));
            String ssid = reader.readLine();
            return ssid != null && !ssid.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            log("ERROR", "broker", e.getMessage());
            return false;
        }
    }

    private boolean ensureWifiConnected() {
        if (isWifiConnected()) {
            return true;
        }
        log("INFO", "broker", "Wi-Fi non connesso: avvio riconnessione...");
        try {
            Process connect = Runtime.getRuntime().exec(new String[] {
                    "bash", "-c",
                    "nmcli dev wifi connect '" + WIFI_SSID + "' password '" + WIFI_PASSWORD + "'"
            });
            connect.waitFor();
        } catch (IOException | InterruptedException e) {
            log("ERROR", "broker", e.getMessage());
            e.printStackTrace();
        }

        return isWifiConnected();
    }

    // Necessario per evitare "doppie accensioni" ravvicinate degli schermi.
    // fuori dai 10 secondi saranno le stazioni a scartarle
    private boolean canWakeVideo() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWakeTime >= WAKE_COOLDOWN) {
            lastWakeTime = currentTime;
            return true;
        }
        return false;
    }

    // Iscrizione ai topic MQTT da cui deve ricevere messaggi
    private void subscribeToTopics() {
        client.subscribe(dataTopic, 0);
        client.subscribe(videoEventTopic, 0);
        client.subscribe(powerTopic, 0);
        client.subscribe(logTopic, 0);
    }

    // Gestione dei messaggi MQTT ricevuti
    private void setupMessageHandler() {
        client.publishHandler(message -> {
            String topic = message.topicName();
            String payload = message.payload().toString("UTF-8");
            String deviceId;

            JsonObject data;
            try {
                data = new JsonObject(payload);
            } catch (Exception e) {
                log("ERROR", "broker", "Payload non valido (non JSON) su topic " + topic + ": " + payload);
                return;
            }

            switch (topic) {
                // topic per ricevere i comandi di spegnimento, sleep e wake
                case powerTopic:
                    String command = data.getString("command");
                    if (command == null) {
                        log("ERROR", "broker", "Comando assente in JSON su topic 'control': " + payload);
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
                        log("ERROR", "broker", "deviceId mancante nel messaggio JSON su topic: " + topic);
                        return;
                    }

                    if (event.equals("triggered")) {
                        handleTriggered(deviceId, data);
                    } else if (event.equals("ended")) {
                        handleEnded(deviceId);
                    }
                    break;
                // topic per ricevere lo stato dei device appena si connettono alla rete (si
                // distinguono per deviceId)
                case dataTopic:
                    deviceId = data.getString("deviceId");
                    if (deviceId == null) {
                        log("ERROR", "broker", "deviceId mancante nel messaggio JSON su topic: " + topic);
                        return;
                    }
                    handleData(deviceId, data);
                    break;
                // topic per ricevere messaggi di log
                case logTopic:
                    deviceId = data.getString("deviceId");
                    String type = data.getString("type");
                    String msg = data.getString("message");
                    if (deviceId == null || type == null || msg == null) {
                        log("ERROR", "broker", "Errore di formattazione nel messaggio di log");
                        return;
                    }
                    log(type, deviceId, msg);
            }

        });
    }

    // Pubblica un messaggio su un topic
    public void publish(String topic, String message) {
        if (client.isConnected()) {
            client.publish(topic, Buffer.buffer(message), MqttQoS.AT_LEAST_ONCE, false, false);
            log("INFO", "broker", "Pubblicato su " + topic + ": " + message);
        } else {
            log("ERROR", "broker", "Client MQTT non connesso!");
        }
    }

    // Gestione Shelly con timer
    public void shellyManager(JsonArray lights) {
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
    }

    // Handlers dei vari eventi

    private void handleTriggered(String deviceId, JsonObject data) {
        log("INFO", "broker", "Trigger ricevuto da " + deviceId + ": " + data.encodePrettily());
        JsonArray lights = data.getJsonArray("lights");

        if (deviceId.equals("videoPlayer-intro") && !introAlreadyTriggered) {
            publish(plafTopic, "LIGHT_MIN");
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
            return;
        }
        if (deviceId.equals("videoPlayer-piano")) {
            pianoAlreadyTriggered = true;
        }

        if (lights != null) {
            shellyManager(lights);
        }
    }

    private void handleEnded(String deviceId) {
        log("INFO", deviceId, "Video terminato");
        if (deviceId.equals("videoPlayer-intro")) {
            if (introAlreadyTriggered) {
                publish(plafTopic, "ON");
                publish(plafTopic, "LIGHT_MAX");
                publish(audioTopic, "ON");
            } else {
                introAlreadyTriggered = true;
            }
        }
    }

    private void handleData(String deviceId, JsonObject data) {

        Boolean online = data.getBoolean("online");

        if (online == null) {
            log("ERROR", deviceId, "Pacchetto di collegamento errato");
            return;
        }

        if (deviceId != null) {
            DeviceStatusManager.updateDeviceStatus(deviceId, data);
            log("INFO", deviceId, "Stato aggiornato: " + (online ? "online" : "offline"));
        }

        if (online) {

            if (deviceId.contains("audioPlayer")) {
                publish(audioTopic, "ON");
            }

            if (deviceId.contains("plafoniera")) {
                publish("bonci/" + deviceId + "/command", "STARTING");
            }

            if (deviceId.contains("videoPlayer")) {
                lastWakeTime = System.currentTimeMillis();
            }
        }
    }

    public void handleControl(String command) {
        switch (command) {
            case "shutdown":
                publish(audioTopic, "SHUTDOWN");
                publish(videoTopic, "SHUTDOWN");
                try {
                    log("INFO", "broker", "Spegnimento Raspberry Pi in corso...");
                    Runtime.getRuntime().exec("sudo shutdown -h now");
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
                if (canWakeVideo()) {
                    publish(videoTopic, "WAKE");
                } else {
                    log("INFO", "broker", "Wake bloccato sui video, cooldown ancora in corso");
                }
                break;
            case "start_presentation":
                introAlreadyTriggered = false;
                pianoAlreadyTriggered = false;
                new Thread(() -> {
                    try {
                        publish(videoTopic, "SLEEP");
                        Thread.sleep(20000);
                        publish(videoTopic, "WAKE");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                publish(plafTopic, "STARTING");
                publish(audioTopic, "ON");
                new Thread(() -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            publish(audioTopic, "VOLUME_UP");
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                break;
            default:
                log("ERROR", "broker", "Comando sconosciuto su topic 'control': " + command);
        }
    }

    public void handleGeneralLight(String command) {
        if (command == null || !lightCommands.contains(command)) {
            log("ERROR", "broker", "Comando mancante o non valido per luce generale");
        } else {
            publish(plafTopic, command);
        }

    }

    public void handleGeneralAudio(String command) {
        if (command == null || !audioCommands.contains(command)) {
            log("ERROR", "broker", "Comando mancante o non valido per audio");
        } else {
            publish(audioTopic, command);
        }
    }

    public void handleGeneralVideo(String command) {
        if (command == null || !videoCommands.contains(command)) {
            log("ERROR", "broker", "Comando mancante o non valido per video");
        } else if (command.equals("WAKE") && !canWakeVideo()) {
            log("INFO", "broker", "Wake bloccato sui video, cooldown ancora in corso");
        } else {
            publish(videoTopic, command);
        }
    }

    public void handleDeviceCommand(String deviceId, String command) {
        if (deviceId == null || command == null) {
            log("ERROR", "broker", "ID dispositivo o comando mancante");
            return;
        }

        if (deviceId.contains("shelly")) {
            if (command.equals("ON") || command.equals("OFF")) {
                publishShellyCommand(deviceId + "/rpc", command.equals("ON") ? true : false);
            }
            return;
        }

        if (deviceId.contains("videoPlayer") && command.equals("WAKE") && !canWakeVideo()) {
            log("INFO", "broker", "Wake bloccato su " + deviceId + ", cooldown ancora in corso");
            return;
        }
        publish("bonci/" + deviceId + "/command", command);
    }
}
