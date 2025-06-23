/*

Codice utilizzato nelle prime fasi per testare se questa soluzione con IR funzionasse
è l'unione del ricevitore e trasmettitore

#include <Arduino.h>
#include <IRremoteESP8266.h>
#include <IRrecv.h>
#include <IRsend.h>
#include <IRutils.h>

// === Pin configurazione ===
#define PIN_RECV 18
#define PIN_SEND1 4
#define PIN_SEND2 5
#define PIN_SEND3 13
#define BUTTON_RECV 17
#define BUTTON_SEND 16
#define BUTTON_NEXT 15

// === Costanti ===
#define MAX_COMMANDS 5

// === Struttura per comando IR ===
struct IRCommand
{
    uint64_t value;
    uint16_t bits;
    decode_type_t protocol;
};

// === Oggetti IR ===
IRrecv irrecv(PIN_RECV);
IRsend irsend1(PIN_SEND1);
IRsend irsend2(PIN_SEND2);
IRsend irsend3(PIN_SEND3);
decode_results results;

// === Variabili ===
IRCommand commands[MAX_COMMANDS];
uint8_t commandCount = 0;
uint8_t currentCommand = 0;

void setup()
{
    Serial.begin(115200);
    delay(200);

    irrecv.enableIRIn();
    irsend1.begin();
    irsend2.begin();
    irsend3.begin();

    pinMode(BUTTON_RECV, INPUT_PULLUP);
    pinMode(BUTTON_SEND, INPUT_PULLUP);
    pinMode(BUTTON_NEXT, INPUT_PULLUP);

    Serial.println("Pronto (ESP32 IR Clone Multi-Comando)");
}

bool isDuplicate(uint64_t value, decode_type_t protocol)
{
    for (int i = 0; i < commandCount; i++)
    {
        if (commands[i].value == value && commands[i].protocol == protocol)
        {
            return true;
        }
    }
    return false;
}

void loop()
{
    // === Ricezione nuovo comando ===
    if (digitalRead(BUTTON_RECV) == LOW)
    {
        Serial.println(">> Ricezione IR attiva...");

        unsigned long start = millis();
        while (millis() - start < 10000)
        {
            if (irrecv.decode(&results))
            {
                if (results.decode_type != decode_type_t::UNKNOWN)
                {
                    if (isDuplicate(results.value, results.decode_type) || results.value == 0xFFFFFFFFFFFFFFFF)
                    {
                        Serial.println("Comando ignorato.");
                    }
                    else if (commandCount < MAX_COMMANDS)
                    {

                        commands[commandCount].value = results.value;
                        commands[commandCount].bits = results.bits;
                        commands[commandCount].protocol = results.decode_type;
                        Serial.print("== Comando salvato [");
                        Serial.print(commandCount);
                        Serial.println("] ==");
                        Serial.print("Protocollo: ");
                        Serial.println(typeToString(results.decode_type));
                        Serial.print("Codice HEX: 0x");
                        Serial.println(results.value, HEX);
                        Serial.print("Bit: ");
                        Serial.println(results.bits);

                        commandCount++;
                    }
                    else
                    {
                        Serial.println("Limite comandi raggiunto!");
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
        Serial.println("Fine ricezione");
        delay(1000);
    }

    // === Invio comando corrente ===
    if (digitalRead(BUTTON_SEND) == LOW)
    {
        if (commandCount > 0)
        {
            IRCommand cmd = commands[currentCommand];
            Serial.print(">> Invia comando [");
            Serial.print(currentCommand);
            Serial.println("]...");
            switch (cmd.protocol)
            {
            case decode_type_t::NEC:
                irsend1.sendNEC(cmd.value, cmd.bits);
                irsend2.sendNEC(cmd.value, cmd.bits);
                irsend3.sendNEC(cmd.value, cmd.bits);
                break;
            case decode_type_t::SONY:
                irsend1.sendSony(cmd.value, cmd.bits);
                irsend2.sendSony(cmd.value, cmd.bits);
                irsend3.sendSony(cmd.value, cmd.bits);
                break;
            case decode_type_t::RC5:
                irsend1.sendRC5(cmd.value, cmd.bits);
                irsend2.sendRC5(cmd.value, cmd.bits);
                irsend3.sendRC5(cmd.value, cmd.bits);
                break;
            case decode_type_t::RC6:
                irsend1.sendRC6(cmd.value, cmd.bits);
                irsend2.sendRC6(cmd.value, cmd.bits);
                irsend3.sendRC6(cmd.value, cmd.bits);
                break;
            default:
                Serial.println("Protocollo non supportato!");
                break;
            }

            Serial.print("Codice inviato: 0x");
            Serial.println(cmd.value, HEX);
        }
        else
        {
            Serial.println("Nessun comando salvato.");
        }
        delay(500);
    }

    // === Scorri comandi (NEXT) ===
    if (digitalRead(BUTTON_NEXT) == LOW)
    {
        if (commandCount > 0)
        {
            currentCommand = (currentCommand + 1) % commandCount;
            IRCommand cmd = commands[currentCommand];

            Serial.print("Comando attivo: [");
            Serial.print(currentCommand);
            Serial.println("]");
            Serial.print("Protocollo: ");
            Serial.println(typeToString(cmd.protocol));
            Serial.print("Codice HEX: 0x");
            Serial.println(cmd.value, HEX);
            Serial.print("Bit: ");
            Serial.println(cmd.bits);
        }
        else
        {
            Serial.println("Nessun comando da ciclare.");
        }
        delay(500); // debounce
    }
}

*/