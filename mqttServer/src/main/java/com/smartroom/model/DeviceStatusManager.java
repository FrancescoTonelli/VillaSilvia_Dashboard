package  com.smartroom.model;

import io.vertx.core.json.JsonObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DeviceStatusManager {

    private static final Map<String, JsonObject> devices = new ConcurrentHashMap<>();

    public static void updateDeviceStatus(String deviceId, JsonObject status) {
        devices.put(deviceId, status);
    }

    public static Map<String, JsonObject> getAllDevices() {
        return devices;
    }

    public static JsonObject getDevice(String deviceId) {
        return devices.get(deviceId);
    }
}
