package museo;

import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        ProcessManager processManager = new ProcessManager();
        MqttHandler mqttHandler = new MqttHandler(vertx, processManager);
        WebServer webServer = new WebServer(vertx, mqttHandler);

        // una volta connesso al broker, fa partire javaFX + pi4j
        mqttHandler.attemptConnection(() -> {
            processManager.startPlayVideoApp(true);
        });

    }

}
