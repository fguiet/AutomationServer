Starting Influxdb docker
------------------------

1. Run AutomationServer/third-party-tools/influxdb/install/prerequisite
2. From influxdb container make old influxdb backup
	influxd backup -database automation /influxdb/restore
	tar -zcvf automation.tar.gz /influxdb/restore
3. Copy /influxdb/restore to /home/fred/database/influxb/backup/YYYYMMDD
4. Remove tar from /influxdb/restore
5. Change container version in file /home/fred/git/AutomationServer/third-party-tools/influxdb/docker/influxdb_dockerbuild
6. Stop influxdb container, remove it and remove images
7. Build new images
8. Create container from image : docker create guiet/influxdb:1.7.1 
9. Copy new influxdb conf file from new container : docker cp 4b0f71acea43:/etc/influxdb/influxdb.conf .
10. Remove fake container (from point 8)
10. Make a diff between old and new conf file
11. copy new conf file in ~/applications/influxdb/conf and empty all ~/applications/influxdb/* (sauf conf)
12. Launch influxdb docker : AutomationServer/third-party-tools/influxdb/docker/influxdb_dockerrun
13. Launch docker exec -it influxdb bash
14. Launch /usr/bin/influx => drop database automation;
15. /usr/bin/influxd restore -online -db automation /influxdb/restore
16. Restoration A REVOIR!!!!
	

