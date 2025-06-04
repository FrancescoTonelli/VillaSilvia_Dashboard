#!/bin/bash

set -e

echo "== Installazione pacchetti necessari =="
sudo apt update
sudo apt install -y hostapd dnsmasq

echo "== Disabilito servizi temporaneamente =="
sudo systemctl stop hostapd
sudo systemctl stop dnsmasq

echo "== Imposto IP statico su wlan0 =="
sudo tee -a /etc/dhcpcd.conf > /dev/null <<EOF

interface wlan0
    static ip_address=192.168.4.1/24
    nohook wpa_supplicant
EOF

echo "== Configuro dnsmasq =="
sudo mv /etc/dnsmasq.conf /etc/dnsmasq.conf.bak 2>/dev/null || true
sudo tee /etc/dnsmasq.conf > /dev/null <<EOF
interface=wlan0
dhcp-range=192.168.4.2,192.168.4.20,255.255.255.0,24h
EOF

echo "== Configuro hostapd =="
sudo tee /etc/hostapd/hostapd.conf > /dev/null <<EOF
interface=wlan0
driver=nl80211
ssid=RaspPi
hw_mode=g
channel=7
ieee80211n=1
wmm_enabled=1
auth_algs=1
wpa=2
wpa_passphrase=VillaRasp1
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP
EOF

echo 'DAEMON_CONF="/etc/hostapd/hostapd.conf"' | sudo tee /etc/default/hostapd

echo "== Abilito servizi =="
sudo systemctl unmask hostapd
sudo systemctl enable hostapd
sudo systemctl enable dnsmasq

echo "== Riavvio il Raspberry... =="
sudo reboot
