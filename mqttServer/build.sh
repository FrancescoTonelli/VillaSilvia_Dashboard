#!/bin/bash

mvn clean install
sudo systemctl restart mqttserver.service