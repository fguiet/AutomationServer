Starting Postgresql docker
------------------------

1. Run AutomationServer/third-party-tools/postgresql/install/prerequisite
2. Make old postgresl backups
	pg_dump --globals-only > users_20180409.sql
	pg_dump automation > automation_dump_20180409.sql
	
3. Put backups in applications/postgresql/restore/
4. Launch postgresql docker : AutomationServer/third-party-tools/postgresql/docker/postgresql_dockerrun
	If /postgresl/data empty a new cluster will be created
	log to container :  docker exec -it postgresql bash
	Restore database
	cd /postgres/restore
	first users, example : psql < users_20180409.sql
	then data, psql < automation_dump_20180409.sql
	

