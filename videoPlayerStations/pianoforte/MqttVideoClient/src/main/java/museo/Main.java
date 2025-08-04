package museo;

import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

        MqttHandler mqttHandler = new MqttHandler(vertx);
        WebServer webServer = new WebServer(vertx, mqttHandler);

    }

}
