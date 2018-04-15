Starting Influxdb docker
------------------------

1. Run AutomationServer/third-party-tools/influxdb/install/prerequisite
2. Make old influxdb backup
	influxd backup -database automation ~/influxdb-backup
	tar -zcvf automation.tar.gz ~/influxdb-backup
3. Untar backup in applications/influxdb/restore/
4. Launch influxdb docker : AutomationServer/third-party-tools/influxdb/docker/influxdb_dockerrun
	Influxdb database will be restored automatically
	

