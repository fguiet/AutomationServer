#!/usr/bin/env bash
set -e

RESTORE_DIR=/influxdb/restore
DATA_DIR=/influxdb/data
META_DIR=/influxdb/meta
INFLUXDB_DAEMON_DIR=/usr/bin/influxd
CONF_FILE=/influxdb/conf/influxdb.conf
LOG_FILE=/influxdb/logs/influxdb.log

#echo "Checking for backup to restore"
#if [ -z "$(ls -A ${RESTORE_DIR})" ]; then
#  echo "Restore dir empty...nuffin to do  !"
#else
#  echo "Restore dir not empty...will try to restore database..."
#  echo "Restoring metastore..."
#  ${INFLUXDB_DAEMON_DIR} restore -metadir ${META_DIR} ${RESTORE_DIR}
#  echo "Restoring database automation with command : ${INFLUXDB_DAEMON_DIR} restore -database automation -datadir ${DATA_DIR} ${RESTORE_DIR}"
#  ${INFLUXDB_DAEMON_DIR} restore -database automation -datadir ${DATA_DIR} ${RESTORE_DIR}
#  echo "Removing restore files"
#  rm /influxdb/restore/*
#fi

echo "Now starting Influxdb..."
${INFLUXDB_DAEMON_DIR} -config ${CONF_FILE} 2>&1 >> ${LOG_FILE}
