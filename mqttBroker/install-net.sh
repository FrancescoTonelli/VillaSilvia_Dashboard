1#!/bin/bash

# Script per creare automaticamente un hotspot Wi-Fi su Raspberry Pi con nmcli
# SSID: RaspPi
# Password: prova

echo "🔧 Installazione NetworkManager..."
sudo apt update
sudo apt install -y network-manager

echo "🚫 Disabilitazione dhcpcd..."
sudo systemctl stop dhcpcd
sudo systemctl disable dhcpcd
sudo systemctl enable NetworkManager

echo "⏳ Riavvio del sistema necessario per completare la configurazione."
read -p "Vuoi riavviare ora? (y/n): " confirm
if [ "$confirm" == "y" ]; then
    sudo reboot
    exit 0
fi
