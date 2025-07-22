#!/bin/bash

cd ../reactDashboard/
./dashboard_build.sh || exit 1
cd ../mqttServer
mvn clean install
sudo systemctl restart mqttServer.service