package com.example.audioplayer;

import org.eclipse.paho.client.mqttv3.*;

public class MQTTClientHandler {

    private MqttClient mqttClient;

    public MQTTClientHandler(String broker, String clientId) throws MqttException {
        mqttClient = new MqttClient(broker, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        mqttClient.connect(options);
    }

    public void subscribe(String topic, MqttCallback callback) throws MqttException {
        mqttClient.setCallback(callback);
        mqttClient.subscribe(topic);
    }

    public void publish(String topic, String message) throws MqttException {
        mqttClient.publish(topic, message.getBytes(), 0, false);
    }

    public void disconnect() throws MqttException {
        mqttClient.disconnect();
    }
}
