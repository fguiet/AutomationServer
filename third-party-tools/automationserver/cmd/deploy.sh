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
touch /applications/automationserver/logs/automationserver-info.log
touch /applications/automationserver/logs/automationserver-error.log
touch /applications/automationserver/logs/automationserver-warn.log
echo "Adding rw rigths to others so tomcat can write in it..."
chmod o+rw /applications/automationserver/logs/automationserver-info.log
chmod o+rw /applications/automationserver/logs/automationserver-error.log
chmod o+rw /applications/automationserver/logs/automationserver-warn.log
echo "Done!"


