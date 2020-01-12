#Grafana

# Migrate from version X to version Y

* Stop Grafana container
* Backup /home/fred/applications/grafana/data folder
* Update Grafana image version to use
* Start Grafana and show logs (`docker logs container_name`), database should be migrated automatically
