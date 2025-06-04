package museo;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class WebServer {

    private final Vertx vertx;

    public WebServer(Vertx vertx, MqttHandler mqttHandler) {
        this.vertx = vertx;

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.post("/event/:type").handler(ctx -> {
            String type = ctx.pathParam("type");

            JsonArray lights = new JsonArray(); // di default vuoto

            Buffer body = ctx.body().buffer();
            if (type.equalsIgnoreCase("triggered") && body != null && body.length() > 0) {
                try {
                    lights = body.toJsonArray();
                } catch (Exception e) {
                    System.err.println("Errore nel parsing del body JSON: " + e.getMessage());
                }
            }

            switch (type.toLowerCase()) {
                case "triggered":
                    mqttHandler.publishTriggered(lights);
                    break;
                case "ended":
                    mqttHandler.publishEnded();
                    break;
                default:
                    System.out.println("Evento non riconosciuto: " + type);
            }

            ctx.response().end("OK");
        });

        vertx.createHttpServer().requestHandler(router).listen(8080, res -> {
            if (res.succeeded()) {
                System.out.println("HTTP server in ascolto su http://localhost:8080");
            } else {
                System.err.println("Errore avvio HTTP server: " + res.cause().getMessage());
            }
        });
    }
}
