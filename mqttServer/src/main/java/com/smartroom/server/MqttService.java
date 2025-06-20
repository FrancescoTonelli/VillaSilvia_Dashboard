package com.smartroom.server;

//import com.smartroom.model.DeviceStatusManager;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.core.buffer.Buffer;

public class MqttService {

    private MqttClient client;

    // topic su cui pubblica dati (quelli delle shelly sono mandati dalle stazioni
    // video)
    private String audioTopic = "smartroom/audio/volume";
    private String plafTopic = "smartroom/plafoniera/luminosità";

    public MqttService(Vertx vertx, String brokerHost, int brokerPort) {
        this.client = MqttClient.create(vertx, new MqttClientOptions());

        client.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("MQTT connected to " + brokerHost + ":" + brokerPort);

                // topic su cui deve ricevere dati
                client.subscribe("smartroom/+/data", 0);
                client.subscribe("smartroom/+/videoPlayer/triggered", 0);
                client.subscribe("smartroom/+/videoPlayer/ended", 0);
                client.subscribe("smartroom/shutdown", 0);
                client.subscribe("smartroom/go_sleep", 0);
                client.subscribe("smartroom/wake_up", 0);

                // gestisce la recezione dei messaggi ricevuti sui topic sopra elencati
                client.publishHandler(message -> {
                    String topic = message.topicName();
                    String payload = message.payload().toString("UTF-8");
                    JsonObject data = new JsonObject(payload);

                    String deviceId = data.getString("deviceId");

                    if (topic.contains("triggered")) {
                        System.out.println("Dispositivo triggerato: " + data.encodePrettily());

                        if (deviceId.equals("videoPlayer1")) {
                            publish(plafTopic, "LIGHT_DOWN");
                            publish(audioTopic, "50");
                        }

                        JsonArray lights = data.getJsonArray("lights");
                        if (lights != null) {
                            shellyManager(lights, vertx);
                        }
                    } else if (topic.contains("ended")) {
                        System.out.println("Video terminato: " + data.encodePrettily());

                        switch (deviceId) {

                            case "videoPlayer1":
                                System.out.println("Ultima stazione video terminata: " + deviceId);
                                publish(plafTopic, "LIGHT_UP");
                                publish(audioTopic, "100");
                                break;

                            default:
                                System.out.println("Altra stazione video terminata " + deviceId);
                        }

                    } else if (topic.contains("data")) {
                        if (deviceId != null) {
                            // DeviceStatusManager.updateDeviceStatus(deviceId, data);
                            System.out.println("Aggiornato stato di: " + deviceId);
                        }

                        if (deviceId.contains("audioPlayer")) {
                            System.out.println("Stazione audio connessa");
                            publish(audioTopic, "TRIGGERED");
                            publish(audioTopic, "100");
                        }

                        if (deviceId.contains("plafoniera")) {
                            System.out.println("Plafoniera connessa");
                            publish(plafTopic, "ON");
                        }

                        if (deviceId.contains("videoPlayer")) {
                            System.out.println("Stazione video connessa");
                        }

                    } else if (topic.contains("shutdown")) {
                        try {
                            Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("Raspberry Pi in spegnimento...");
                    } else if (topic.contains("go_sleep")) {
                        publish(audioTopic, "0");
                        publish(plafTopic, "OFF");
                    } else if (topic.contains("wake_up")) {
                        publish(audioTopic, "100");
                        publish(plafTopic, "ON");
                    }
                });

            } else {
                System.err.println("MQTT connection failed: " + s.cause().getMessage());
            }
        });

    }

    // metodo usato per pubblicare un messaggio su un topic
    public void publish(String topic, String message) {
        if (client.isConnected()) {
            client.publish(
                    topic,
                    Buffer.buffer(message),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            System.out.println("Pubblicato su " + topic + ": " + message);
        } else {
            System.err.println("Client MQTT non connesso!");
        }
    }

    // metodo usato per gestire i tempi di accensione e spegnimento delle shelly
    public void shellyManager(JsonArray lights, Vertx vertx) {
        lights.forEach(light -> {
            JsonObject lightObj = (JsonObject) light;

            String lightId = lightObj.getString("id");
            int delaySeconds = lightObj.getInteger("delay", 0);
            Integer offAfter = lightObj.getInteger("offAfter", 0);
            String lightTopic = lightId + "/rpc";

            if (delaySeconds >= 1) {
                vertx.setTimer(delaySeconds * 1000L, tid -> {
                    JsonObject accendi = new JsonObject()
                            .put("id", 1)
                            .put("src", "server")
                            .put("method", "Switch.Set")
                            .put("params", new JsonObject()
                                    .put("id", 0)
                                    .put("on", true));

                    publish(lightTopic, accendi.encode());

                    System.out.println("Luce Shelly accesa su topic: " + lightTopic);
                });
            }

            if (offAfter >= 1) {
                vertx.setTimer(offAfter * 1000L, offTid -> {
                    JsonObject spegni = new JsonObject()
                            .put("id", 1)
                            .put("src", "server")
                            .put("method", "Switch.Set")
                            .put("params", new JsonObject()
                                    .put("id", 0)
                                    .put("on", false));

                    publish(lightTopic, spegni.encode());

                    System.out.println("Luce Shelly spenta su topic: " + lightTopic);
                });
            }

        });
    }

}
