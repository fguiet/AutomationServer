#! /bin/sh

echo "Stopping Tomcat docker container..."
docker stop tomcat
echo "Copying new WebApp..."
cp /home/fred/applications/automationserver/bin/automationserver-webapp.war /home/fred/applications/tomcat/deploy/
echo "Starting Tomcat docker container..."
docker start tomcat
