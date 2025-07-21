package com.smartroom.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;

import com.smartroom.model.DeviceStatusManager;

import java.util.List;
import java.util.ArrayList;

public class WebServer {

    public static void start(Vertx vertx) {
        Router router = Router.router(vertx);
        List<ServerWebSocket> wsClients = new ArrayList<>();

        // CORS
        router.route().handler(
            CorsHandler.create().addOrigin("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedHeader("Content-Type")
        );

        router.route().handler(BodyHandler.create());

        // Static webroot
        router.route("/*").handler(StaticHandler.create("webroot"));

        // REST APIs
        router.get("/devices").handler(ctx -> {
            JsonObject response = new JsonObject();
            DeviceStatusManager.getAllDevices().forEach(response::put);
            ctx.response()
                .putHeader("content-type", "application/json")
                .end(response.encodePrettily());
        });

        router.get("/devices/:id").handler(ctx -> {
            String id = ctx.pathParam("id");
            var device = DeviceStatusManager.getDevice(id);
            if (device != null) {
                ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(device.encodePrettily());
            } else {
                ctx.response().setStatusCode(404).end("Dispositivo non trovato");
            }
        });

        router.post("/command").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");

            if (command == null) {
                ctx.response().setStatusCode(400).end("Comando mancante");
                return;
            }

            System.out.println("Comando ricevuto: " + command);

            new MqttService(vertx, "localhost", 1883).handleControl(command);
            ctx.response().end("Comando ricevuto: " + command);
        });

        router.post("/devices/:id/command").handler(ctx -> {
            String id = ctx.pathParam("id");
            JsonObject body = ctx.body().asJsonObject();
            String action = body.getString("action");
            Object value = body.getValue("value");
            if (action == null) {
                ctx.response().setStatusCode(400).end("Manca action");
                return;
            }
            new MqttService(vertx, "localhost", 1883)
                .handleDeviceCommand(id, action, value);
            ctx.response().end("Comando " + action + " su " + id);
        });


        // HTTP + WebSocket server
        vertx.createHttpServer()
            .webSocketHandler(ws -> {
                if (ws.path().equals("/ws")) {
                    wsClients.add(ws);
                    System.out.println("Client WebSocket connesso");

                    ws.closeHandler(v -> {
                        System.out.println("Client WebSocket disconnesso");
                        wsClients.remove(ws);
                    });
                } else {
                    ws.reject();
                }
            })
            .requestHandler(router)
            .listen(8080, http -> {
                if (http.succeeded()) {
                    System.out.println("Server in ascolto su http://localhost:8080");
                } else {
                    System.err.println("Errore: " + http.cause());
                }
            });

        // Callback WebSocket
        DeviceStatusManager.setOnUpdateCallback((JsonObject update) -> {
            for (ServerWebSocket client : wsClients) {
                if (!client.isClosed()) {
                    client.writeTextMessage(update.encode());
                }
            }
        });
    }
}
