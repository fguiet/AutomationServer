#!/usr/bin/env bash
#set -e

#echo $@

postgresql_server () {
   rm -Rf /postgresql/data/* 
   /usr/lib/postgresql/9.6/bin/initdb -D /postgresql/data 
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

