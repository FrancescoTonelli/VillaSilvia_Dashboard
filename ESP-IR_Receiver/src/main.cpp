#include <Arduino.h>
#include <IRrecv.h>
#include <IRutils.h>

#define PIN_RECV 18    // Un pin digitale disponibile sull'ESP32
#define BUTTON_RECV 16 // Un altro pin digitale connesso al pulsante

const int MAX_CODES = 10;
unsigned long savedCodes[MAX_CODES];
uint8_t savedBits[MAX_CODES];
decode_type_t savedProtocols[MAX_CODES];
int codeCount = 0;

IRrecv irrecv(PIN_RECV, 1024, 15); // buffer size, timeout in ms
decode_results results;

void setup()
{
  Serial.begin(115200);

  irrecv.enableIRIn(); // Avvia ricezione IR

  pinMode(BUTTON_RECV, INPUT_PULLUP);
  delay(5000);
  Serial.print("Start");
}

void loop()
{

  if (digitalRead(BUTTON_RECV) == LOW)
  {
    Serial.printf(">> Ricezione IR attiva... (Slot %d)\n", codeCount + 1);
    unsigned long start = millis();

    while (millis() - start < 10000)
    {
      if (irrecv.decode(&results))
      {
        if (results.decode_type != decode_type_t::UNKNOWN)
        {
          if (codeCount < MAX_CODES)
          {
            savedCodes[codeCount] = results.value;
            savedBits[codeCount] = results.bits;
            savedProtocols[codeCount] = results.decode_type;
            codeCount++;

            Serial.println("== Codice salvato ==");
            Serial.print("Protocollo: ");
            Serial.println(typeToString(results.decode_type));
            Serial.print("Codice HEX: 0x");
            Serial.println(results.value, HEX);
            Serial.print("Bit: ");
            Serial.println(results.bits);
          }
          else
          {
            Serial.println("Memoria piena. Non è possibile salvare altri codici.");
          }
        }
        else
        {
          Serial.println("Segnale sconosciuto.");
        }

        irrecv.resume();
        break;
      }
      delay(10);
    }
    delay(1000);
  }
}