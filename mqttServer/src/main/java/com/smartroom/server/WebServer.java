package com.smartroom.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
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

    public static void start(Vertx vertx, MqttService mqttService) {
        Router router = Router.router(vertx);
        List<ServerWebSocket> wsClients = new ArrayList<>();

        // CORS
        router.route().handler(
                CorsHandler.create().addOrigin("*")
                        .allowedMethod(HttpMethod.GET)
                        .allowedMethod(HttpMethod.POST)
                        .allowedHeader("Content-Type"));

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

            mqttService.log("INFO", "broker", "Comando ricevuto: " + command);

            mqttService.handleControl(command);
            ctx.response().end("Comando ricevuto: " + command);
        });

        router.post("/devices/:id/command").handler(ctx -> {
            String id = ctx.pathParam("id");
            JsonObject body = ctx.body().asJsonObject();
            String action = body.getString("action");
            if (action == null) {
                ctx.response().setStatusCode(400).end("Manca action");
                return;
            }
            mqttService.handleDeviceCommand(id, action);

            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Comando " + action + " inviato a " + id);
        });

        router.post("/light/general/command").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");
            if (command == null) {
                ctx.response().setStatusCode(400).end("Comando mancante per luce generale");
                return;
            }

            mqttService.handleGeneralLight(command);

            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Comando luce generale: " + command);
        });

        router.post("/audio/general/command").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");
            if (command == null) {
                ctx.response().setStatusCode(400).end("Comando mancante per audio ");
                return;
            }

            mqttService.handleGeneralAudio(command);

            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Comando audio generale: " + command);
        });

        router.post("/video/general/command").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");
            if (command == null) {
                ctx.response().setStatusCode(400).end("Comando mancante per video ");
                return;
            }

            mqttService.handleGeneralVideo(command);

            ctx.response()
                    .putHeader("content-type", "text/plain")
                    .end("Comando video generale: " + command);
        });

        router.post("/shelly/:id/command").handler(ctx -> {
            String id = ctx.pathParam("id");
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");
            if (command == null || !(command.equals("ON") || command.equals("OFF"))) {
                ctx.response().setStatusCode(400).end("Manca parametro 'command' valido (ON/OFF)");
                return;
            }
            mqttService.handleDeviceCommand(id, command);
            ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("deviceId", id)
                            .put("command", command)
                            .encodePrettily());
        });

        router.post("/shelly/command").handler(ctx -> {
            JsonObject body = ctx.body().asJsonObject();
            String command = body.getString("command");
            if (command == null || !(command.equals("ON") || command.equals("OFF"))) {
                ctx.response().setStatusCode(400).end("Manca parametro 'command' valido (ON/OFF)");
                return;
            }
            DeviceStatusManager.getAllDevices().keySet().stream()
                    .filter(dev -> dev.contains("shelly"))
                    .forEach(dev -> mqttService.handleDeviceCommand(dev, command));
            ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                            .put("allShelly", true)
                            .put("command", command)
                            .encodePrettily());
        });

        // HTTP + WebSocket server
        vertx.createHttpServer()
                .webSocketHandler(ws -> {
                    if (ws.path().equals("/ws")) {
                        wsClients.add(ws);

                        // Invia all'istante gli ultimi 100 log a questo client
                        try {
                            JsonArray lastLogs = LogAgent.fetchLastLogs(100);
                            JsonObject payload = new JsonObject()
                                    .put("type", "logs_initial")
                                    .put("payload", lastLogs);
                            ws.writeTextMessage(payload.encodePrettily());
                        } catch (Exception e) {
                            // non interrompere la connessione se non trova DB; loggare
                            mqttService.log("ERROR", "broker", "Impossibile recuperare ultimi log: " + e.getMessage());
                        }

                        // Gestione messaggi inbound dal client (es. richiesta logs)
                        ws.textMessageHandler(message -> {
                            try {
                                JsonObject obj = new JsonObject(message);
                                String action = obj.getString("action");
                                if ("request_logs".equals(action)) {
                                    int limit = obj.getInteger("limit", 100);
                                    JsonArray logs = LogAgent.fetchLastLogs(limit);
                                    ws.writeTextMessage(new JsonObject().put("type", "logs_initial").put("payload", logs).encodePrettily());
                                }
                            } catch (Exception ex) {
                                // ignora messaggi non JSON
                            }
                        });

                        ws.closeHandler(v -> {
                            wsClients.remove(ws);
                        });
                    } else {
                        ws.reject();
                    }
                })
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        mqttService.log("INFO", "broker", "Web server in ascolto su http://localhost:8080");
                    } else {
                        mqttService.log("ERROR", "broker", "Errore Web Server: " + http.cause());
                    }
                });

        // Broadcast: quando DeviceStatusManager manda un aggiornamento, inoltralo ai WS clients
        DeviceStatusManager.setOnUpdateCallback((JsonObject update) -> {
            JsonObject wrapper = new JsonObject().put("type", "device").put("payload", update);
            for (ServerWebSocket client : wsClients) {
                if (!client.isClosed()) {
                    client.writeTextMessage(wrapper.encodePrettily());
                }
            }
        });

        // Quando arriva un nuovo log dal LogAgent, inoltralo ai WS clients
        LogAgent.setOnNewLogCallback((JsonObject logEntry) -> {
            JsonObject wrapper = new JsonObject().put("type", "log").put("payload", logEntry);
            for (ServerWebSocket client : wsClients) {
                if (!client.isClosed()) {
                    client.writeTextMessage(wrapper.encodePrettily());
                }
            }
        });

    }
}
