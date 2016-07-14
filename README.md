NAME
  AutomationServer

DESCRIPTION

  AutomationServer is a personal home automation project. 
  It is both a software and electronic project.
  Currently, it monitors temperatures in my house by controlling independently my electric heaters (wanted temperatures can be set day by day, hour by hour and even minutes by minutes) . 
  Electrical pieces of information from my electric provider are also collected. 
  Moreover, it launches automatically my water boiler at night when electrical cost is less expensive. 
  ALl collected data (temperatue, humidity, electric consumption) are saved in a database.
  
  This project is based on :
  
    - PostgreSQL 9.4
    - Java 1.8
    - Arduino
    - DHT22 Sensor
    - XBee
    - Raspberry first edition
    - Handmade electonic stuffs (to collect data from my electric provider, to control my heaters)
  
FEATURES
  - Monitor, room by room, temperature and humidity of the house
  - Temperatures level in each room automatically monitored through a week personal defined schedule
  - Responsive and interactive web-based dashboard (aggregate all monitored data, temperture can be changed room by room, etc)
  - All pieces of information collected are saved in a PostgreSQL database 
  - Automatic SMS notification when a sensor is disconnected
  - Monitor real-time house electric consumption
  - Automatic start of my water boiler when electric cost is less expensive (mostly during the night)

INSTALLATION
  TODO

LICENSE
    Copyright (c) 2015-2016 Frédéric Guiet  - All rights reserved.

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
