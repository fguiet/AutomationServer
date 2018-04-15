#! /bin/sh

export NAME="automationserver"
export EXEC="/usr/bin/jsvc"
export HOME_PATH="/applications/automationserver"
export LOG4J="log4j.configuration=file:${HOME_PATH}/config/log4j.xml"
#export JAVA_HOME="/usr/java/jdk1.8.0_111"
export JAVA_LIBRARY_PATH="java.library.path=/applications/automationserver"
export CLASS_PATH="${HOME_PATH}/*"
export ARGS="-Dautomationserver.config.path=/applications/automationserver/config/automationserver.properties"
export LOG_OUT="${HOME_PATH}/logs/${NAME}.out"
export LOG_ERR="${HOME_PATH}/logs/${NAME}.err"
export PID="${HOME_PATH}/${NAME}.pid"
export CLASS=fr.guiet.automationserver.AutomationServer

rm /var/lock/LCK..ttyUSB0
$EXEC -home "$JAVA_HOME" -cp "$CLASS_PATH" -D"${JAVA_LIBRARY_PATH}" -D"${LOG4J}" $ARGS -outfile "${LOG_OUT}" -errfile "${LOG_ERR}" -pidfile "${PID}" $1 $CLASS

