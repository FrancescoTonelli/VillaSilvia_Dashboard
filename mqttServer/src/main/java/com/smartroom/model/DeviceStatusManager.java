package  com.smartroom.model;

import io.vertx.core.json.JsonObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.Map;

public class DeviceStatusManager {

    private static final Map<String, JsonObject> devices = new ConcurrentHashMap<>();
    private static Consumer<JsonObject> onUpdateCallback;

    public static void setOnUpdateCallback(Consumer<JsonObject> callback) {
        onUpdateCallback = callback;
    }

    public static void updateDeviceStatus(String deviceId, JsonObject data) {
        devices.put(deviceId, data);

        if (onUpdateCallback != null) {
            JsonObject updateMsg = data.copy()
            .put("deviceId", deviceId);

            onUpdateCallback.accept(updateMsg);
        }
    }

    public static Map<String, JsonObject> getAllDevices() {
        return devices;
    }

    public static JsonObject getDevice(String deviceId) {
        return devices.get(deviceId);
    }
}
