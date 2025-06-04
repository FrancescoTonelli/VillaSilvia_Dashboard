async function fetchDevices() {
    try {
        const response = await fetch('/devices');
        const data = await response.json();

        const tbody = document.querySelector("#device-table tbody");
        tbody.innerHTML = "";

        Object.entries(data).forEach(([deviceId, info]) => {
            const row = document.createElement("tr");

            const onlineStatus = info.online ? "online" : "offline";
            const timestamp = new Date(info.timestamp).toLocaleString();

            row.innerHTML = `
          <td>${deviceId}</td>
          <td>${info.deviceName || '-'}</td>
          <td class="${onlineStatus}">${onlineStatus}</td>
          <td>${info.activeVideo || '-'}</td>
          <td>${info.ipAddress || '-'}</td>
          <td>${Math.floor((info.uptime || 0) / 1000)}</td>
          <td>${info.freeMemoryMB || '-'}</td>
          <td>${info.os || '-'}</td>
          <td>${timestamp}</td>
        `;

            tbody.appendChild(row);
        });

    } catch (error) {
        console.error("Errore nel fetch dei dispositivi:", error);
    }
}

fetchDevices();
