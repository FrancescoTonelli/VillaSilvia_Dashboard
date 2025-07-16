package com.smartroom.server;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import com.smartroom.model.DeviceStatusManager;
import io.vertx.core.json.JsonObject;

public class WebServer {

    public static void start(Vertx vertx) {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        // Serve tutte le risorse statiche da /webroot
        router.route("/*").handler(StaticHandler.create("webroot"));

        // Endpoint JSON API (es. /devices)
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
                ctx.response()
                        .setStatusCode(404)
                        .end("Dispositivo non trovato");
            }
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        System.out.println("🌐 Server HTTP in ascolto su http://localhost:8080");
                    } else {
                        System.err.println("Errore HTTP: " + http.cause());
                    }
                });
    }
}
