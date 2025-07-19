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

const char *deviceId = "plafoniera1";

const char *publicTopic = "bonci/plafoniere/command";           // topic in ascolto tutte le plafoniere
String privateTopic = "bonci/" + String(deviceId) + "/command"; // topic in ascolto solo questa plafoniera
const char *dataTopic = "bonci/online_data";                    // topic per pubblicare lo stato online

const uint32_t CODE_ON = 0x807F807F;
const uint32_t CODE_OFF = 0x807F807F;
const uint32_t CODE_LIGHT_UP = 0x807FC03F;
const uint32_t CODE_LIGHT_DOWN = 0x807F10EF;
const uint32_t CODE_WARM_UP = 0x807FA05F;
const uint32_t CODE_COLD_UP = 0x807F609F;

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

// === Callback MQTT ===
void callback(char *topic, byte *payload, unsigned int length)
{
  String command;
  for (unsigned int i = 0; i < length; i++)
  {
    command += (char)payload[i];
  }
  Serial.println(command);

  Serial.print("Messaggio ricevuto su topic [");
  Serial.print(topic);
  Serial.print("]: ");
  Serial.print("Comando :");
  Serial.print(command);

  if (command == "STARTING")
  {
    irsend.sendNEC(CODE_ON, 32);
    delay(250);
    for (int i = 0; i < 9; i++)
    {
      irsend.sendNEC(CODE_LIGHT_UP, 32);
      delay(500);
    }
    for (int i = 0; i < 3; i++)
    {
      irsend.sendNEC(CODE_LIGHT_DOWN, 32);
      delay(500);
    }
    for (int i = 0; i < 8; i++)
    {
      irsend.sendNEC(CODE_WARM_UP, 32);
      delay(500);
    }
  }
  else if (command == "ON")
  {
    irsend.sendNEC(CODE_ON, 32);
  }
  else if (command == "OFF")
  {
    irsend.sendNEC(CODE_OFF, 32);
  }
  else if (command == "LIGHT_UP")
  {
    for (int i = 0; i < 6; i++)
    {
      irsend.sendNEC(CODE_LIGHT_UP, 32);
      delay(500);
    }
  }
  else if (command == "LIGHT_DOWN")
  {
    for (int i = 0; i < 6; i++)
    {
      irsend.sendNEC(CODE_LIGHT_DOWN, 32);
      delay(500);
    }
  }
  else if (command == "WARM_UP")
  {
    irsend.sendNEC(CODE_WARM_UP, 32);
  }
  else if (command == "COLD_UP")
  {
    irsend.sendNEC(CODE_COLD_UP, 32);
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
    if (client.connect(deviceId))
    {
      Serial.println("connesso");

      // Iscrizione al topic di controllo
      client.subscribe(publicTopic);
      client.subscribe(privateTopic.c_str());

      // === Invio messaggio JSON ===
      StaticJsonDocument<200> doc;
      doc["online"] = true;
      doc["deviceId"] = deviceId;
      doc["ip"] = WiFi.localIP().toString();
      doc["timestamp"] = millis(); // tempo dal boot in ms

      char buffer[256];
      size_t n = serializeJson(doc, buffer);
      client.publish(dataTopic, buffer, n);

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
