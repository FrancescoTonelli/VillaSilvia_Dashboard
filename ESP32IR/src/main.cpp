#include <WiFi.h>
#include <PubSubClient.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <IRutils.h>
#include <ArduinoJson.h>
#include <Arduino.h>

// === Configurazione Wi-Fi e MQTT ===
const char *ssid = "Bonci_WiFi";
const char *password = "BonciRoom1";
const char *mqtt_server = "192.168.0.2";

WiFiClient espClient;
PubSubClient client(espClient);

// === Trasmettitore IR ===
#define PIN_SEND1 4
IRsend irsend(PIN_SEND1);

// === Setup Wi-Fi ===
void setup_wifi()
{
  delay(10);
  Serial.println();
  Serial.print("Connessione a ");
  Serial.println(ssid);
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  delay(1000);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(10000);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connesso");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

void on()
{
  irsend.sendNEC(0x807F807F, 32);
  Serial.println("Inviato comando IR: ON");
}

void off()
{
  irsend.sendNEC(0x807F807F, 32);
  Serial.println("Inviato comando IR: OFF");
}

void light_up()
{
  irsend.sendNEC(0x807FC03F, 32);
  Serial.println("Inviato comando IR: LIGHT UP");
}

void light_down()
{
  irsend.sendNEC(0x807F10EF, 32);
  Serial.println("Inviato comando IR: LIGHT DOWN");
}

void warm_up()
{
  irsend.sendNEC(0x807FA05F, 32);
  Serial.println("Inviato comando IR: WARM UP");
}

void cold_up()
{
  irsend.sendNEC(0x807F609F, 32);
  Serial.println("Inviato comando IR: COLD UP");
}

// === Callback MQTT ===
void callback(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Messaggio ricevuto su topic [");
  Serial.print(topic);
  Serial.print("]: ");

  String command;
  for (unsigned int i = 0; i < length; i++)
  {
    command += (char)payload[i];
  }
  Serial.println(command);

  if (command == "STARTING")
  {
    on();
    delay(250);
    for (int i = 0; i < 9; i++)
    {
      light_up();
      delay(500);
    }
    for (int i = 0; i < 3; i++)
    {
      light_down();
      delay(500);
    }
    for (int i = 0; i < 8; i++)
    {
      warm_up();
      delay(500);
    }
  }
  else if (command == "ON")
  {
    on();
  }
  else if (command == "OFF")
  {
    off();
  }
  else if (command == "LIGHT_UP")
  {
    for (int i = 0; i < 6; i++)
    {
      light_up();
      delay(500);
    }
  }
  else if (command == "LIGHT_DOWN")
  {
    // dalla luminosità massima si abbassa di 5 tacche
    for (int i = 0; i < 6; i++)
    {
      light_down();
      delay(500);
    }
  }
  else if (command = "WARM_UP")
  {
    warm_up();
  }
  else if (command = "COLD_UP")
  {
    cold_up();
  }
  else
  {
    Serial.println("Comando sconosciuto!");
  }
}

// === Riconnessione MQTT ===
void reconnect()
{
  while (!client.connected())
  {
    Serial.print("Tentativo connessione MQTT...");
    if (client.connect("ESP32Client"))
    {
      Serial.println("connesso");

      // Iscrizione al topic di controllo
      client.subscribe("bonci/plafoniera/command");

      // === Invio messaggio JSON ===
      StaticJsonDocument<200> doc;
      doc["online"] = true;
      doc["deviceId"] = "plafoniera";
      doc["ip"] = WiFi.localIP().toString();
      doc["timestamp"] = millis(); // tempo dal boot in ms

      char buffer[256];
      size_t n = serializeJson(doc, buffer);
      client.publish("bonci/online_data", buffer, n);

      Serial.println("Messaggio di connessione inviato");
    }
    else
    {
      Serial.print("Errore, rc=");
      Serial.print(client.state());
      Serial.println(" riprovo in 10 secondi");
      delay(10000);
    }
  }
}

void setup()
{
  Serial.begin(115200);
  irsend.begin();

  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void loop()
{
  if (WiFi.status() != WL_CONNECTED)
  {
    setup_wifi();
  }
  if (!client.connected())
  {
    reconnect();
  }
  client.loop();
}
