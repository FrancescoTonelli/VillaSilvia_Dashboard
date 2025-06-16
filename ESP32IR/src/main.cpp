#include <WiFi.h>
#include <PubSubClient.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <IRutils.h>
#include <ArduinoJson.h>

// === Configurazione Wi-Fi e MQTT ===
const char *ssid = "RaspPi";
const char *password = "VillaRasp1";
const char *mqtt_server = "10.42.0.1";

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
  WiFi.disconnect(true); // Reset connessioni precedenti
  delay(1000);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
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
  Serial.print("Messaggio ricevuto su topic [");
  Serial.print(topic);
  Serial.print("]: ");

  String command;
  for (unsigned int i = 0; i < length; i++)
  {
    command += (char)payload[i];
  }
  Serial.println(command);

  // Comandi supportati: ON, OFF, LIGHT UP, LIGHT DOWN
  if (command == "ON")
  {
    irsend.sendNEC(0x807F807F, 32);
    Serial.println("Inviato comando IR: ON");
  }
  else if (command == "OFF")
  {
    irsend.sendNEC(0x807F807F, 32);
    Serial.println("Inviato comando IR: OFF");
  }
  else if (command == "LIGHT_UP")
  {
    for (int i = 0; i < 5; i++)
    {
      irsend.sendNEC(0x807FC03F, 32);
      Serial.println("Inviato comando IR: LIGHT UP");
      delay(500);
    }
  }
  else if (command == "LIGHT_DOWN")
  {
    for (int i = 0; i < 5; i++)
    {
      irsend.sendNEC(0x807F10EF, 32);
      Serial.println("Inviato comando IR: LIGHT DOWN");
      delay(500);
    }
  }
  else
  {
    Serial.println("Comando sconosciuto!");
  }

  /*VERSIONE CON CODICI MANDATI DAL BROKER
  StaticJsonDocument<200> doc;
  DeserializationError error = deserializeJson(doc, jsonStr);
  if (error)
  {
    Serial.print("Errore parsing JSON: ");
    Serial.println(error.c_str());
    return;
  }

  String protocol = doc["protocol"];
  uint64_t value = strtoull(doc["value"], NULL, 16);
  uint16_t bits = doc["bits"];

  Serial.printf("Invio IR - Protocollo: %s, Valore: 0x%llX, Bit: %u\n", protocol.c_str(), value, bits);

  if (protocol == "NEC")
  {
    irsend.sendNEC(value, bits);
  }
  else
  {
    Serial.println("Protocollo non supportato!");
  }*/
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
      client.subscribe("smartroom/plafoniera/luminosità");

      // === Invio messaggio JSON al topic smartroom/plafoniera/data ===
      StaticJsonDocument<200> doc;
      doc["online"] = true;
      doc["deviceId"] = "plafoniera";
      doc["ip"] = WiFi.localIP().toString();
      doc["timestamp"] = millis(); // tempo dal boot in ms

      char buffer[256];
      size_t n = serializeJson(doc, buffer);
      client.publish("smartroom/plafoniera/data", buffer, n);

      Serial.println("Messaggio di connessione inviato");
    }
    else
    {
      Serial.print("Errore, rc=");
      Serial.print(client.state());
      Serial.println(" riprovo in 5 secondi");
      delay(5000);
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
  if (!client.connected())
  {
    reconnect();
  }
  client.loop();
}
