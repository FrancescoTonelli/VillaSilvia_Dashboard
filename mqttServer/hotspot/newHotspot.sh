#!/bin/sh

sudo apt-get update
sudo apt-get install dnsmasq hostapd -y
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y netfilter-persistent iptables-persistent

sudo raspi-config nonint do_wifi_country IT

sudo sed -i '$ainterface wlan0\nstatic ip_address=10.20.1.1/24\nnohook wpa_supplicant' /etc/dhcpcd.conf

echo "net.ipv4.ip_forward=1" | sudo tee /etc/sysctl.d/routed-ap.conf

sudo iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
sudo netfilter-persistent save

sudo mv /etc/dnsmasq.conf /etc/dnsmasq.conf.orig
sudo tee /etc/dnsmasq.conf > /dev/null <<EOF
interface=wlan0
dhcp-range=10.20.1.5,10.20.1.100,255.255.255.0,24h
domain=ap
address=/rpi.ap/10.20.1.1
EOF

sudo tee /etc/hostapd/hostapd.conf > /dev/null <<EOF
country_code=IT
interface=wlan0
ssid=RaspPi
hw_mode=g
channel=6
macaddr_acl=0
auth_algs=1
ignore_broadcast_ssid=0
wpa=2
wpa_passphrase=VillaRasp1
wpa_key_mgmt=WPA-PSK
rsn_pairwise=CCMP
EOF

echo "DAEMON_CONF=\"/etc/hostapd/hostapd.conf\"" | sudo tee /etc/default/hostapd > /dev/null

sudo systemctl unmask hostapd
sudo systemctl enable hostapd
