package museo;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class MqttHandler {

    private final String wifiSSID = "Bonci_WiFi";      
    private final String wifiPassword = "BonciRoom1";

    private final MqttClient client;
    private final String deviceId = "videoPlayer-intro";
    private final Vertx vertx;
    private final ProcessManager manager;

    private final String commandTopic = "bonci/videoPlayer/command"; // broker -> videoPlayer
    private final String privateCommandTopic = "bonci/" + deviceId + "/command"; // borker -> videoPlayer (private)
    private final String eventTopic = "bonci/videoPlayer/event"; // videoPlayer -> broker
    private final String dataTopic = "bonci/online_data"; // videoPlayer -> broker
    private final String logTopic = "bonci/log"; // canale log

    private boolean isAwaken;

    public MqttHandler(Vertx vertx) {
        this.vertx = vertx;
        this.manager = new ProcessManager(this);

        JsonObject lwt = new JsonObject()
                .put("online", false)
                .put("deviceId", deviceId)
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

    public void attemptConnection() {

        if (!ensureWifiConnected()) {
            System.err.println("Nessuna connessione Wi-Fi. Riprovo tra 10s...");
            vertx.setTimer(10_000, id -> attemptConnection());
            return;
        }

        client.connect(1883, "192.168.0.2", ar -> {

            if (ar.succeeded()) {
                System.out.println("Connesso al broker Mqtt");
                subscribeToTopics();
                publishData();

                client.closeHandler(v -> {
                    isAwaken = false;
                    System.err.println("Connessione MQTT persa. Riprovo tra 10s...");
                    manager.stopPlayVideoApp();
                    vertx.setTimer(10_000, id -> attemptConnection());

                });

                vertx.executeBlocking(promise -> {
                    isAwaken = true;
                    manager.startPlayVideoApp(true);
                    promise.complete();
                }, false, res -> {
                });
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
        client.subscribe(privateCommandTopic, MqttQoS.AT_LEAST_ONCE.value());

        client.publishHandler(msg -> {
            String payload = msg.payload().toString();
            String topic = msg.topicName();
            log("INFO", "Ricevuto comando MQTT: " + payload + " su topic " + topic);

            if (topic.equals(commandTopic) || topic.equals(privateCommandTopic)) {
                switch (payload) {
                    case "SHUTDOWN":
                        try {
                            Process process = Runtime.getRuntime().exec("sudo shutdown -h now");
                        } catch (Exception e) {
                            log("ERROR", e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "SLEEP":
                        isAwaken = false;
                        vertx.executeBlocking(promise -> {
                            manager.stopPlayVideoApp();
                            promise.complete();
                        }, false, res -> {
                            // Niente da fare nel callback, solo per non bloccare
                        });
                        break;
                    case "WAKE":
                        if (!isAwaken) {
                            isAwaken = true;
                            vertx.executeBlocking(promise -> {
                                manager.startPlayVideoApp(false);
                                promise.complete();
                            }, false, res -> {
                            });
                        } else {
                            log("INFO", "WAKE bloccato, dispositivo già attivo");
                        }
                        break;

                    default:
                        log("ERROR", "Comando non riconosciuto");
                        break;
                }
            }

        });
    }

    public void log(String type, String message) {
        if (client.isConnected()) {

            client.publish(
                    logTopic,
                    Buffer.buffer(
                        new JsonObject()
                            .put("deviceId", deviceId)
                            .put("type", type)
                            .put("message", message)
                            .encode()
                    ),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);

            if (type.equals("ERROR")) {
                System.err.println(message);
            }
            else {
                System.out.println(message);
            }
        } else {
            System.err.println("MQTT non connesso (log)");
        }
    }

}
