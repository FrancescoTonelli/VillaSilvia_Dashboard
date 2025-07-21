#!/bin/bash

cd ../reactDashboard/
./dashboard_build.sh
cd ../mqttServer
mvn clean install
sudo systemctl restart mqttserver.service