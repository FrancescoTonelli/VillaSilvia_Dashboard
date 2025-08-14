package com.smartroom.server;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Consumer;

public class LogAgent {
    private JDBCClient client;
    private static final String DEFAULT_DB_PATH = "/home/admin/Desktop/condivisa/VillaSilvia_Dashboard/mqttServer/logs.db";
    private static final String SCHEMA_RESOURCE = "logs.sql";

    private static volatile Consumer<JsonObject> onNewLogCallback = null;

    public static void setOnNewLogCallback(Consumer<JsonObject> cb) {
        onNewLogCallback = cb;
    }

    public LogAgent(Vertx vertx) {
        final String envUrl = System.getenv("JDBC_URL");
        final String dbUrl;
        if (envUrl == null || envUrl.isBlank()) {
            dbUrl = "jdbc:sqlite:" + DEFAULT_DB_PATH;
        } else {
            dbUrl = envUrl;
        }

        JsonObject config = new JsonObject()
                .put("url", dbUrl)
                .put("driver_class", "org.sqlite.JDBC");
        this.client = JDBCClient.createShared(vertx, config);

        vertx.executeBlocking(promise -> {
            try {
                ensureSchemaInDb(dbUrl, SCHEMA_RESOURCE);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, res -> {
            if (!res.succeeded()) {
                throw new RuntimeException("Failed to initialize DB schema", res.cause());
            }
        });
    }

    private static void ensureSchemaInDb(String dbUrl, String resourcePath) throws Exception {
        try (InputStream in = LogAgent.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
            }
            String ddl = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute(ddl);
            }
        }
    }

    private static String getDbUrl() {
        final String envUrl = System.getenv("JDBC_URL");
        if (envUrl == null || envUrl.isBlank()) {
            return "jdbc:sqlite:" + DEFAULT_DB_PATH;
        }
        return envUrl;
    }

    public static JsonArray fetchLastLogs(int limit) throws Exception {
        String dbUrl = getDbUrl();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT type, timestamp, device, message FROM logs ORDER BY timestamp DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            JsonArray arr = new JsonArray();
            while (rs.next()) {
                JsonObject entry = new JsonObject()
                        .put("type", rs.getString("type"))
                        .put("timestamp", rs.getLong("timestamp"))
                        .put("device", rs.getString("device"))
                        .put("message", rs.getString("message"));
                arr.add(entry);
            }
            return arr;
        }
    }

    public void log(String type, String device, String message) {
        final long ts = System.currentTimeMillis();

        if (client == null) {
            var stream = type.equals("ERROR") ? System.err : System.out;
            stream.println("LOG (no-db): " + device + ": " + type + " | " + message);

            JsonObject j = new JsonObject()
                    .put("type", type)
                    .put("timestamp", ts)
                    .put("device", device)
                    .put("message", message);
            if (onNewLogCallback != null) onNewLogCallback.accept(j);
            return;
        }

        JsonArray params = new JsonArray()
            .add(type)
            .add(ts)
            .add(device)
            .add(message);

        client.updateWithParams(
            "INSERT INTO logs(type,timestamp,device,message) VALUES(?,?,?,?)",
            params,
            ar -> {
                if (ar.failed()) {
                    ar.cause().printStackTrace();
                } else {
                    JsonObject j = new JsonObject()
                            .put("type", type)
                            .put("timestamp", ts)
                            .put("device", device)
                            .put("message", message);
                    if (onNewLogCallback != null) onNewLogCallback.accept(j);
                }
            }
        );
    }
}
