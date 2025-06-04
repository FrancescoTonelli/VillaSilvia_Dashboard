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

    private String audioTopic = "smartroom/audio";
    private String plafTopic = "smartroom/plafoniere";
    private MqttClient client;

    public MqttService(Vertx vertx, String brokerHost, int brokerPort) {
        this.client = MqttClient.create(vertx, new MqttClientOptions());

        client.connect(brokerPort, brokerHost, s -> {
            if (s.succeeded()) {
                System.out.println("MQTT connected to " + brokerHost + ":" + brokerPort);

                client.subscribe("smartroom/+/data", 0, handler -> {
                    if (handler.succeeded()) {
                        System.out.println("Subscribed to topic smartroom/+/data");
                    }
                });

                client.subscribe("smartroom/+/triggered", 0, handler -> {
                    if (handler.succeeded()) {
                        System.out.println("Subscribed to topic smartroom/+/triggered");
                    }
                });

                client.subscribe("smartroom/+/ended", 0, handler -> {
                    if (handler.succeeded()) {
                        System.out.println("Subscribed to topic smartroom/+/ended");
                    }
                });
                client.subscribe("smartroom/shutdown", 0, handler -> {
                    if (handler.succeeded()) {
                        System.out.println("Subscribed to topic smartroom/+/ended");
                    }
                });

                publish(plafTopic, "100");
                publish(audioTopic, "TRIGGERED");
                publish(audioTopic, "VOLUME_100");

                client.publishHandler(message -> {
                    String topic = message.topicName();
                    String payload = message.payload().toString("UTF-8");
                    JsonObject data = new JsonObject(payload);

                    String deviceId = data.getString("deviceId");

                    if (topic.contains("triggered")) {
                        System.out.println("Dispositivo triggerato: " + data.encodePrettily());

                        if (deviceId.equals("first-device")) {
                            publish(plafTopic, "50");
                            publish(audioTopic, "VOLUME_50");
                        }

                        JsonArray lights = data.getJsonArray("lights");
                        if (lights != null) {
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

                                        client.publish(
                                                lightTopic,
                                                Buffer.buffer(accendi.encode()),
                                                MqttQoS.AT_LEAST_ONCE,
                                                false, false);

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

                                        client.publish(
                                                lightTopic,
                                                Buffer.buffer(spegni.encode()),
                                                MqttQoS.AT_LEAST_ONCE,
                                                false, false);

                                        System.out.println("Luce Shelly spenta su topic: " + lightTopic);
                                    });
                                }

                            });
                        }
                    } else if (topic.contains("ended")) {
                        System.out.println("Video terminato: " + data.encodePrettily());

                        switch (deviceId) {

                            case "last-device":
                                System.out.println("Dispositivo (ended): " + deviceId);
                                publish(plafTopic, "100");
                                publish(audioTopic, "VOLUME_100");
                                break;

                            default:
                                System.out.println("Dispositivo sconosciuto (ended): " + deviceId);
                        }

                    } else if (topic.contains("data")) {
                        if (deviceId != null) {
                            // DeviceStatusManager.updateDeviceStatus(deviceId, data);
                            System.out.println("Aggiornato stato di: " + deviceId);
                        }
                    } else if (topic.contains("shutdown")) {
                        try {
                            Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        System.out.println("Raspberry Pi in spegnimento...");
                    }
                });

            } else {
                System.err.println("MQTT connection failed: " + s.cause().getMessage());
            }
        });

    }

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

}
