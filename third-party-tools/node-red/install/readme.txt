#Installer des nouveaux noeuds

# Open a shell in the container
docker exec -it mynodered /bin/bash

# Once inside the container, npm install the nodes in /data
cd /data
npm install node-red-node-smooth
exit

# Restart the container to load the new nodes
docker stop mynodered
docker start mynodered

#Noeuds nécessaires:

npm install node-red-contrib-bigtimer
