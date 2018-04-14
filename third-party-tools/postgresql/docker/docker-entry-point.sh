#!/usr/bin/env bash
#set -e

#echo $@

postgresql_server () {

   if [ ! "$(ls -A $DIR)" ]; then
      rm -Rf /postgresql/data/* 
       /usr/lib/postgresql/9.6/bin/initdb --locale=en_GB.UTF-8  -D /postgresql/data
      echo "host all  all    0.0.0.0/0  md5" >> /postgresql/data/pg_hba.conf
      echo "listen_addresses='*'" >> /postgresql/data/postgresql.conf
   fi

   /usr/lib/postgresql/9.6/bin/postgres -D /postgresql/data > /postgresql/logs/postgresql.log 2>&1   
}

echo "Starting PostgreSQL 9.6 server..."
postgresql_server

#exec "$@"

##!/usr/bin/env bash
#set -e

#if [ "$1" = 'postgres' ]; then
#    chown -R postgres "$PGDATA"

#    if [ -z "$(ls -A "$PGDATA")" ]; then
#        gosu postgres initdb
#    fi

#    exec gosu postgres "$@"
#fi
#
#exec "$@"

