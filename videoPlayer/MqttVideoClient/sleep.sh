#!/bin/bash

# 1. Spegni la TV tramite HDMI-CEC (assume device 0 = TV)
echo "standby 0" | cec-client -s -d 1

# 2. Spegni l'uscita HDMI del Raspberry
#OUTPUT=$(xrandr | grep " connected" | cut -d ' ' -f1)
#xrandr --output "$OUTPUT" --off

# 3. Disattiva interfaccia grafica
#sudo systemctl isolate multi-user.target

# 4. Imposta la CPU su modalità risparmio energetico
#for CPUFREQ in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  #echo "powersave" | sudo tee "$CPUFREQ"
#done

# 5. Disattiva LED (facoltativo)
# echo 0 | sudo tee /sys/class/leds/led0/brightness
# echo 0 | sudo tee /sys/class/leds/led1/brightness 2>/dev/null

# 6. Disattiva Wi-Fi/Bluetooth (facoltativo)
# sudo rfkill block wifi
# sudo rfkill block bluetooth

#echo "[INFO] Sleep attivo. In ascolto su MQTT..."
