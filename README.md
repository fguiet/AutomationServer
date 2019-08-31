NAME
    
  AutomationServer

DESCRIPTION

  AutomationServer is a personal home automation project. 
  It is both a software and electronic project.
  Currently, it monitors temperatures in my house by controlling independently my electric heaters (wanted temperatures can be set day by day, hour by hour and even minutes by minutes) . 
  Electrical pieces of information from my electric provider are also collected. 
  Moreover, it launches automatically my water boiler at night when electrical cost is less expensive. My electric rollershutters open and close automatically regarding sunset/sunrise hours.
  All collected data (temperatue, humidity, electric consumption) are saved in a database (PostgreSQL and InfluxDB).
  
  This project is based on :
  
    - PostgreSQL 9.6
    - Open JDK 9
    - Oracle Jersey Restful framework
    - cron4j
    - SunriseSunsetCalculator
    - Ajax Daypilot Scheduling (great scheduling framework)
    - Tomcat 9
    - Wemos ESP8266 Wifi module (not anymore but it used to, replaced by nRF51822 BLE module)
    - nRF51822 BLE Module
    - Arduino (not anymore, replaced by Wemos ESP8266)
    - DHT22 Sensor (temperature and humidity), not suitable with deep sleep (replaced by DS18B20)
    - XBee (not anymore but it used to, replaced by Wemos WiFi module)
    - Raspberry Pi 3 (Hypriot OS)
    - Docker
    - Grafana
    - OpenHab 2
    - InfluxDB 1.7.x
    - Node-Red
    - Mqtt (mosquitto)
    - Gammu
    - Handmade electonic stuffs (to collect data from my electric provider, to control my heaters)
  
FEATURES

  - Monitor, room by room, temperature and humidity of the house
  - Outside temperature and humidity
  - New TV series downloaded SMS notification (via Sickrage and Sabnzbd)
  - Mail notification (SMS is sent when the postman is opening my mailbox)
  - Basement humidity monitoring (automatic ventilation regarding defined humidity range)
  - Temperatures level in each room automatically monitored through a week personal defined schedule
  - OpenHab 2 User Interface (linked mainly with Mqtt)
  - All pieces of information collected are saved in a PostgreSQL and InfluxDB database 
  - Automatic SMS notifications
  - Monitor real-time house electric consumption
  - Automatic start of my water boiler when electric cost is less expensive (mostly during night)
  - All data (electric consumption, humidity, temperature) can be visualized via custom Grafana dashboards
  - All data are published to a Mqtt broker via various topics, so various client subscriber software can access data in real-time
  - Automatic computing of next electricity bill
  - Automatic rollershutters management (via sunset/sunrise api)

INSTALLATION

  TODO but everything runs under Docker (Raspberry Hypriot OS) 

LICENSE

  Copyright (c) 2015-2019 Frédéric Guiet  - All rights reserved.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see < http://www.gnu.org/licenses/ >.
