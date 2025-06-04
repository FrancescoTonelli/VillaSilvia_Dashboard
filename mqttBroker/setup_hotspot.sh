echo "🧹 Rimozione vecchia connessione RaspPi (se esiste)..."
nmcli connection delete RaspPi 2>/dev/null

echo "📡 Creazione della connessione Hotspot..."
nmcli connection add type wifi ifname wlan0 con-name RaspPi autoconnect yes ssid RaspPi

echo "⚙️ Configurazione modalità AP e indirizzamento..."
nmcli connection modify RaspPi 802-11-wireless.mode ap
nmcli connection modify RaspPi 802-11-wireless.band bg
nmcli connection modify RaspPi 802-11-wireless.channel 6
nmcli connection modify RaspPi ipv4.method shared

echo "🔒 Impostazione sicurezza WPA2..."
nmcli connection modify RaspPi 802-11-wireless-security.key-mgmt wpa-psk
nmcli connection modify RaspPi 802-11-wireless-security.psk "VillaRasp1"
nmcli connection modify RaspPi 802-11-wireless-security.proto rsn
nmcli connection modify RaspPi 802-11-wireless-security.pairwise ccmp
nmcli connection modify RaspPi 802-11-wireless-security.group ccmp

echo "🚀 Avvio dell'hotspot..."
nmcli connection down RaspPi
nmcli connection up RaspPi

echo "✅ Hotspot Wi-Fi 'RaspPi' attivo! Password: VillaRasp1"
