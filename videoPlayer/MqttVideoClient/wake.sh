#!/bin/bash

# 1. Riattiva HDMI con risoluzione automatica
OUTPUT=$(xrandr | grep " connected" | cut -d ' ' -f1)
xrandr --output "$OUTPUT" --auto

# 2. Riavvia esplicitamente il display manager (lightdm di default)
sudo systemctl restart lightdm
#sleep 5

# 3. Ripristina la CPU in modalità dinamica
for CPUFREQ in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
  echo "ondemand" | sudo tee "$CPUFREQ"
done

# 4. Riaccendi la TV tramite HDMI-CEC
echo "on 0" | cec-client -s -d 1
echo "as" | cec-client -s -d 1

# 5. Riattiva LED (facoltativo)
# echo 1 | sudo tee /sys/class/leds/led0/brightness
# echo 1 | sudo tee /sys/class/leds/led1/brightness 2>/dev/null

# 6. Riattiva Wi-Fi/Bluetooth (facoltativo)
# sudo rfkill unblock wifi
# sudo rfkill unblock bluetooth


