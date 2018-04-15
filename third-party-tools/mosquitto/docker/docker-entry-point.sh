#!/usr/bin/env bash
set -e

MOSQUITTO_DAEMON_DIR=/usr/sbin/mosquitto
CONF_FILE=/mosquitto/conf/mosquitto.conf

echo "Now starting Mosquitto..."
${MOSQUITTO_DAEMON_DIR} -c ${CONF_FILE}
