#! /bin/sh

echo "Getting new version from Git..."
cd /applications/java/src//AutomationServer/automationserver/ASParent
git pull
echo "Compiling new version..."
mvn clean package
echo "Removing old version..."
rm -f /applications/automationserver/bin/*
echo "Copying new version..."
cp /applications/java/src/AutomationServer/third-party-lib/native/*.so /applications/automationserver/bin/
cp /applications/java/src/AutomationServer/automationserver/ASMain/dist/automationserver-main-bin/lib/* /applications/automationserver/bin/
cp /applications/java/src/AutomationServer/automationserver/ASWebApp/target/automationserver-webapp.war /applications/automationserver/bin
echo "Removing old logs..."
rm -f /applications/automationserver/logs/*
echo "Creating empty logs files..."
touch /applications/automationserver/logs/ErrorAutomationServer.log
touch /applications/automationserver/logs/InfoAutomationServer.log
touch /applications/automationserver/logs/WarnAutomationServer.log
echo "Adding rw rigths to others so tomcat can write in it..."
chmod o+rw /applications/automationserver/logs/ErrorAutomationServer.log
chmod o+rw /applications/automationserver/logs/InfoAutomationServer.log
chmod o+rw /applications/automationserver/logs/WarnAutomationServer.log
echo "Done!"


