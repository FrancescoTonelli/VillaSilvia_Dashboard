LOGFILE="/home/aricci/Desktop/condivisa/MqttVideoClient/log.txt"
{
  echo "==== WAKE MODE ===="
  echo "Data: $(date)"
  echo "Uptime: $(uptime -p)"
  echo "Temperatura: $(vcgencmd measure_temp)"
  echo "RAM:"
  free -h
  echo "CPU Usage (top):"
  top -bn1 | head -n 15
  echo "CPU Frequenza: $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq) Hz"
  echo "Stato rfkill:"
  rfkill list
  echo ""
} >> "$LOGFILE"