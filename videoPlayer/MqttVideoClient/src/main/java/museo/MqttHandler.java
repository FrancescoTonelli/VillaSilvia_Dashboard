package museo;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

import io.netty.handler.codec.mqtt.MqttQoS;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MqttHandler {

    private final MqttClient client;
    private final String deviceId = "videoPlayer-grammofono1";
    private final Vertx vertx;
    private final ProcessManager manager;

    private final String commandTopic = "bonci/videoPlayer/command"; // broker -> videoPlayer
    private final String eventTopic = "bonci/videoPlayer/event"; // videoPlayer -> broker
    private final String dataTopic = "bonci/online_data"; // videoPlayer -> broker

    public MqttHandler(Vertx vertx) {
        this.vertx = vertx;
        this.manager = new ProcessManager();
        client = MqttClient.create(vertx, new MqttClientOptions());
        attemptConnection();
    }

    public void attemptConnection() {
        client.connect(1883, "192.168.0.2", ar -> {
            if (ar.succeeded()) {
                System.out.println("Connesso al broker Mqtt");
                subscribeToTopics();
                publishData();

                client.closeHandler(v -> {
                    System.err.println("Connessione MQTT persa. Riprovo tra 10s...");
                    manager.stopPlayVideoApp();
                    vertx.setTimer(10_000, id -> attemptConnection());

                });

                manager.startPlayVideoApp(true);
            } else {
                System.err.println("Dopo 60 secondi il tentativo di connessione è fallito" + ar.cause().getMessage());
                attemptConnection();
            }
        });
    }

    // Metodo per comunicare al broker l'inizio della riproduzione del video
    public void publishTriggered(JsonArray lights) {
        if (client.isConnected()) {
            JsonObject payload = new JsonObject()
                    .put("deviceId", deviceId)
                    .put("event", "triggered")
                    .put("lights", lights);
            client.publish(eventTopic,
                    Buffer.buffer(payload.encode()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            System.out.println("Messaggio MQTT 'triggered' pubblicato");
        } else {
            System.out.println("MQTT non connesso (trigger)");
        }
    }

    // Metodo per comunicare al broker la conclusione della riproduzione del video
    public void publishEnded() {
        if (client.isConnected()) {
            JsonObject payload = new JsonObject()
                    .put("deviceId", deviceId)
                    .put("event", "ended");
            client.publish(eventTopic,
                    Buffer.buffer(payload.encode()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            System.out.println("Messaggio MQTT 'ended' pubblicato");
        } else {
            System.out.println("MQTT non connesso (ended)");
        }
    }

    // Quando il dispositivo si connette al broker gli manda un messaggio con i suoi
    // dati per fargli capire che si è connesso

    public void publishData() {
        if (client.isConnected()) {
            JsonObject payload = new JsonObject()
                    .put("online", true)
                    .put("deviceId", deviceId)
                    .put("freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024))
                    .put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024))
                    .put("os", System.getProperty("os.name"))
                    .put("timestamp", System.currentTimeMillis());

            client.publish(
                    dataTopic,
                    Buffer.buffer(payload.encode()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);

            System.out.println("MQTT online status pubblicato");
        } else {
            System.out.println("MQTT non connesso (data)");
        }
    }

    // Metodo per iscriversi al topic su cui deve ricevere dati e gestisce anche la
    // ricezione dei messaggi su di esso
    public void subscribeToTopics() {
        client.subscribe(commandTopic, MqttQoS.AT_LEAST_ONCE.value());

        client.publishHandler(msg -> {
            String payload = msg.payload().toString();
            String topic = msg.topicName();
            System.out.println("Ricevuto comando MQTT: " + payload + "su topic " + topic);

            if (topic.equals(commandTopic)) {
                switch (payload) {
                    case "SHUTDOWN":
                        try {
                            Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case "SLEEP":
                        vertx.executeBlocking(promise -> {
                            manager.stopPlayVideoApp();
                            promise.complete();
                        }, false, res -> {
                            // Niente da fare nel callback, solo per non bloccare
                        });
                        break;
                    case "WAKE":
                        vertx.executeBlocking(promise -> {
                            manager.startPlayVideoApp(false);
                            promise.complete();
                        }, false, res -> {
                        });
                        break;

                    default:
                        System.out.println("Comando non ricoosciuto");
                        break;
                }
            }

        });
    }

}
