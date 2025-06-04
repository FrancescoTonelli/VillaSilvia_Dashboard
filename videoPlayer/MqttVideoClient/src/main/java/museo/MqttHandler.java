package museo;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import io.netty.handler.codec.mqtt.MqttQoS;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.InetAddress;

public class MqttHandler {

    private final MqttClient client;
    private final String deviceId = "pi1";
    private final String deviceName = "Vetrina Centrale";
    private static final long appStartTime = System.currentTimeMillis();
    private final Vertx vertx;
    private final ProcessManager manager;

    public MqttHandler(Vertx vertx, ProcessManager manager) {
        this.vertx = vertx;
        this.manager = manager;
        client = MqttClient.create(vertx, new MqttClientOptions());
    }

    public void attemptConnection(Runnable onConnected) {
        client.connect(1883, "10.42.0.1", ar -> {
            if (ar.succeeded()) {
                System.out.println("MQTT connesso");
                client.closeHandler(v -> {
                    System.out.println("MQTT disconnesso!");
                    // attemptConnection(onConnected);
                });
                this.subscribeToSleepCommands();
                onConnected.run();
            } else {
                System.err.println("Connessione fallita: " + ar.cause().getMessage());
                vertx.setTimer(10000, id -> attemptConnection(onConnected));
            }
        });
    }

    public void publishTriggered(JsonArray lights) {
        if (client.isConnected()) {
            String topic = "smartroom/" + deviceId + "/triggered";
            JsonObject payload = new JsonObject()
                    .put("deviceName", deviceName)
                    .put("deviceId", deviceId)
                    .put("event", "triggered")
                    .put("lights", lights);
            client.publish(topic,
                    Buffer.buffer(payload.encode()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            System.out.println("Messaggio MQTT 'triggered' pubblicato");
        } else {
            System.out.println("MQTT non connesso (trigger)");
        }
    }

    public void publishEnded() {
        if (client.isConnected()) {
            String topic = "smartroom/" + deviceId + "/ended";
            JsonObject payload = new JsonObject()
                    .put("deviceName", deviceName)
                    .put("deviceId", deviceId)
                    .put("event", "ended");
            client.publish(topic,
                    Buffer.buffer(payload.encode()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            System.out.println("Messaggio MQTT 'ended' pubblicato");
        } else {
            System.out.println("MQTT non connesso (ended)");
        }
    }

    /*
     * public void publishOnlineStatus(String activeVideoName) {
     * if (client.isConnected()) {
     * String topic = "smartroom/" + deviceId + "/data";
     * JsonObject payload = new JsonObject()
     * .put("online", true)
     * .put("deviceId", deviceId)
     * .put("deviceName", deviceName)
     * .put("activeVideo", activeVideoName)
     * .put("ipAddress", getLocalIpAddress())
     * .put("uptime", System.currentTimeMillis() - appStartTime)
     * .put("freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024))
     * .put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024))
     * .put("os", System.getProperty("os.name"))
     * .put("timestamp", System.currentTimeMillis());
     * 
     * client.publish(topic,
     * Buffer.buffer(payload.encode()),
     * MqttQoS.AT_LEAST_ONCE,
     * false,
     * true); // retained = true
     * System.out.println("MQTT online status pubblicato con video: " +
     * activeVideoName);
     * } else {
     * System.out.println("MQTT non connesso (data)");
     * }
     * }
     */

    public void subscribeToSleepCommands() {
        client.subscribe("smartroom/" + deviceId + "/cmd", MqttQoS.AT_LEAST_ONCE.value(), ar -> {
            if (ar.succeeded()) {
                System.out.println("Iscritto a smartroom/" + deviceId + "/cmd");
            }
        });
        client.subscribe("smartroom/shutdown", MqttQoS.AT_LEAST_ONCE.value(), ar -> {
            if (ar.succeeded()) {
                System.out.println("Iscritto a smartroom/shutdown");
            }
        });

        client.publishHandler(msg -> {
            String payload = msg.payload().toString().toLowerCase().trim();
            String topic = msg.topicName();
            System.out.println("Ricevuto comando MQTT: " + payload + "su topic " + topic);

            if (topic.contains("shutdown")) {

                try {
                    Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println("Raspberry Pi in spegnimento...");

            } else if (topic.contains("cmd")) {
                switch (payload) {
                    case "sleep":
                        vertx.executeBlocking(promise -> {
                            executeScript("/home/aricci/Desktop/condivisa/MqttVideoClient/log.sh");
                            manager.stopPlayVideoApp();
                            promise.complete();
                        }, false, res -> {
                            // Niente da fare nel callback, solo per non bloccare
                        });
                        break;

                    case "wake":
                        vertx.executeBlocking(promise -> {
                            executeScript("/home/aricci/Desktop/condivisa/MqttVideoClient/log.sh");
                            manager.startPlayVideoApp(false);
                            promise.complete();
                        }, false, res -> {
                        });
                        break;

                    default:
                        System.out.println("Comando non riconosciuto");
                }
            }

        });
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    private String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public void executeScript(String path) {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", path);
            pb.inheritIO(); // facoltativo: mostra output su console Java
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Errore eseguendo lo script: " + path);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
