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
import java.io.IOException;

import com.smartroom.model.DeviceStatusManager;

public class MqttService {

    private final Vertx vertx;
    private final String brokerHost;
    private final int brokerPort;
    private final MqttClient client;

    private static final String WIFI_SSID = "Bonci_WiFi";
    private static final String WIFI_PASSWORD = "BonciRoom1";

    private final String audioTopic = "bonci/audioPlayer/command"; // broker -> audioPlayer (ON,OFF,VOLUME...)
    private final String plafTopic = "bonci/plafoniere/command"; // broker -> plafoniere (ON,OFF,LIGHT_UP......)
    private final String videoTopic = "bonci/videoPlayer/command"; // broker -> videoPlayer
    private final String videoEventTopic = "bonci/videoPlayer/event"; // videoPlayer -> broker (TRIGGERED,ENDED)
    private final String powerTopic = "bonci/power/command"; // pannello -> broker (SHUTDOWN,SLEEP,WAKE_UP)
    private final String dataTopic = "bonci/online_data"; // dispositivi -> broker (DEVICEID,IP......)

    private Boolean pianoAlreadyTriggered = false;
    private Boolean introAlreadyTriggered = false;

    private long lastWakeTime = 0;
    private final long WAKE_COOLDOWN = 10000;

    public MqttService(Vertx vertx, String brokerHost, int brokerPort) {
        this.vertx = vertx;
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.client = MqttClient.create(vertx, new MqttClientOptions());

        attemptConnection();

        // Failsafe periodico: se MQTT si disconnette o il Wi-Fi cade
        vertx.setPeriodic(10000, id -> {
            if (!client.isConnected() || !isWifiConnected()) {
                System.err.println("Verifica connettività fallita: riavvio connessione...");
                attemptConnection();
            }
        });
    }

    private void attemptConnection() {
        if (!ensureWifiConnected()) {
            System.err.println("Wi-Fi non connesso. Riprovo tra 10s...");
            vertx.setTimer(10000, id -> attemptConnection());
            return;
        }

        System.out.println("Wi-Fi OK, connetto a MQTT...");
        client.connect(brokerPort, brokerHost, res -> {
            if (res.succeeded()) {
                System.out.println("MQTT connesso a " + brokerHost + ":" + brokerPort);
                subscribeToTopics();
                setupMessageHandler();
                client.closeHandler(v -> {
                    System.err.println("Connessione MQTT persa");
                    attemptConnection();
                });
            } else {
                System.err.println("Connessione MQTT fallita: " + res.cause().getMessage());
                vertx.setTimer(5000, tid -> attemptConnection());
            }
        });
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
            return false;
        }
    }

    private boolean ensureWifiConnected() {
        if (isWifiConnected()) {
            return true;
        }

        System.out.println("Wi-Fi non connesso: avvio riconnessione...");
        try {
            Process connect = Runtime.getRuntime().exec(new String[]{
                "bash", "-c",
                "nmcli dev wifi connect '" + WIFI_SSID + "' password '" + WIFI_PASSWORD + "'"
            });
            connect.waitFor();
        } catch (IOException | InterruptedException e) {
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
        System.out.println("Luce Shelly " + (on ? "accesa" : "spenta") + " su topic: " + topic);
    }

    // Handlers dei vari eventi

    private void handleTriggered(String deviceId, JsonObject data) {
        System.out.println("Trigger ricevuto da " + deviceId + ": " + data.encodePrettily());
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
        System.out.println("Video terminato su " + deviceId);
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
            lastWakeTime = System.currentTimeMillis();
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
                if (canWakeVideo()) {
                    publish(videoTopic, "WAKE");
                } else {
                    System.out.println("Wake bloccato sui video, cooldown ancora in corso");
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
                    } catch(InterruptedException e) {
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
                System.out.println("Presentazione avviata: luce generale accesa, audio acceso, video riavviati");
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
                publish(audioTopic, "VOLUME_UP");
                System.out.println("Audio in aumento");
                break;
            case "AUDIO_DOWN":
                publish(audioTopic, "VOLUME_DOWN");
                System.out.println("Audio in diminuzione");
                break;
            case "SHUTDOWN":
                publish(audioTopic, "SHUTDOWN");
                System.out.println("Dispositivo audio spento");
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
                break;
            case "WAKE":
                if (canWakeVideo()) {
                    publish(videoTopic, "WAKE");
                } else {
                    System.out.println("Wake bloccato sui video, cooldown ancora in corso");
                }
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

        if (deviceId.contains("shelly")) {
            if (command.equals("ON") || command.equals("OFF")) {
                publishShellyCommand(deviceId + "/rpc", command.equals("ON") ? true : false);
            }
            return;
        }

        if (deviceId.contains("videoPlayer") && command.equals("WAKE") && !canWakeVideo()) {
            System.out.println("Wake bloccato su " + deviceId + ", cooldown ancora in corso");
            return;
        }
        publish("bonci/" + deviceId + "/command", command);
        System.out.println("Comando " + command + " inviato a " + deviceId + ".");
    }
}
