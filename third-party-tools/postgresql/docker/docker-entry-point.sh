#!/usr/bin/env bash
set -e

PG_DATA_DIR=/postgresql/data
PG_LOG_DIR=/postgresql/logs
PG_BIN_DIR=/usr/lib/postgresql/9.6/bin

echo "Checking for PostgreSQL cluster creation"
if [ -z "$(ls -A ${PG_DATA_DIR})" ]; then
  echo "PG Data folder empty...creating new PostgreSQL cluster"
  ${PG_BIN_DIR}/initdb --locale=en_GB.UTF-8 -D ${PG_DATA_DIR}
  
  echo "Configuring pg_hba.conf..."
  echo "host all  all    0.0.0.0/0  md5" >> ${PG_DATA_DIR}/pg_hba.conf
  echo "Configuring postgresql.conf..."
  echo "listen_addresses='*'" >> /${PG_DATA_DIR}/postgresql.conf
else
  echo "PG Data not empty...will start PG now"	
fi

echo "Starting PostgreSQL 9.6 server..."
${PG_BIN_DIR}/postgres -D ${PG_DATA_DIR} > ${PG_LOG_DIR}/postgresql.log 2>&1   

