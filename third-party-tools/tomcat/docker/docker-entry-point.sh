#!/usr/bin/env bash
set -e

TOMCAT_DEPLOY_FOLDER=/tomcat/deploy
TOMCAT_WEBAPP_DIR=/applications/apache-tomcat-9.0.7/webapps

echo "Checking whether webapp deployment is necessary"
if [ -z "$(ls -A ${TOMCAT_DEPLOY_FOLDER})" ]; then
  echo "Deploy folder empty...skipping..."  
else
  echo "Deploying new webapp..."
  echo "Removing old webapp..."
  rm -Rf ${TOMCAT_WEBAPP_DIR}
  mkdir ${TOMCAT_WEBAPP_DIR}
  echo "Copying new wepapp..."
  cp ${TOMCAT_DEPLOY_FOLDER}/automationserver-webapp.war ${TOMCAT_WEBAPP_DIR}/
fi

echo "Starting Tomcat server..."
exec $@

