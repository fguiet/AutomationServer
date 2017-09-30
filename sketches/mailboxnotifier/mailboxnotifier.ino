 /**** 
 * Mailbox Notifier
 * 
 * F. Guiet 
 * Creation           : 20170211
 * Last modification  : 20170930
 * 
 * Version            : 1.1
 * 
 * History            : 10 - First version
 *                      11 - add OTA update
 *                      
 * Note               : OTA only work correcly only and only if a hard reset is done AFTER serial port upload otherwise ESP will fail to start up on when OTA update occurs
 *                      https://github.com/esp8266/Arduino/issues/1782                     
 *                      https://github.com/esp8266/Arduino/issues/1017
 *                      
 */

#include <PubSubClient.h>
#include <ESP8266WiFi.h>
#include <ArduinoJson.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>

/**** VARIABLES ***/
StaticJsonBuffer<300> JSONBuffer;
char message_buff[100];
WiFiClient espClient;
PubSubClient client(espClient);
ADC_MODE(ADC_VCC); //vcc read mode
/**** END VARIABLES ***/


/**** DEFINE ***/
#define MAX_RETRY 100
String SENSORID =  "6"; 
//Mqtt settings
#define MQTT_SERVER "192.168.1.25"
#define MQTT_CLIENT_ID "MailboxSensor"
#define mqtt_server "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""
#define mailboxnotifier_topic "/guiet/mailbox/gotmail"
// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";
//Mesure with a voltmeter and calculate that the number mesured from ESP is correct
#define VCC_ADJ 1.038485804
#define PIN_LED 12
const int CURRENT_FIRMWARE_VERSION = 14;
String CHECK_FIRMWARE_VERSION_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getversion/" + SENSORID;
String BASE_FIRMWARE_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getfirmware/" + SENSORID;
IPAddress ip_wemos (192,168,1,42); 
IPAddress gateway_ip ( 192,168,1,1);
IPAddress subnet_mask(255, 255, 255,0);
/**** END DEFINE ****/


void setup() {

  // Serial
  Serial.begin(115200);

  yield();

  //wait a little
  delay(200);
    
  client.setServer(mqtt_server, 1883); 
  pinMode(PIN_LED, OUTPUT);
  digitalWrite(PIN_LED, LOW);

  delay(200);

  makeLedBlink(2, 100);
  Serial.println("Module initialized...");

  delay(200);
  yield();

  connectToWifi();

  //Check for firmware update
  checkForUpdate();

  connectToMqtt();

  yield();
}

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(PIN_LED, HIGH);
    delay(millisecond);
    digitalWrite(PIN_LED, LOW);
    delay(millisecond);
  }
}

void connectToWifi() 
{
  byte mac[6];

  WiFi.forceSleepWake();
  WiFi.disconnect();
  Serial.println("Connecting to WiFi...");
  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    delay(500);
    Serial.print(".");
    retry++;    
  }

  if (retry >= MAX_RETRY) {
    makeLedBlink(5,100);
    goDeepSleep();
  }
  
  Serial.println ( "" );
  Serial.print ( "Connected to " );
  Serial.println ( ssid );
  Serial.print ( "IP address: " );
  Serial.println ( WiFi.localIP() );
  
  WiFi.macAddress(mac);
  Serial.print("MAC: ");
  Serial.print(mac[5],HEX);
  Serial.print(":");
  Serial.print(mac[4],HEX);
  Serial.print(":");
  Serial.print(mac[3],HEX);
  Serial.print(":");
  Serial.print(mac[2],HEX);
  Serial.print(":");
  Serial.print(mac[1],HEX);
  Serial.print(":");
  Serial.println(mac[0],HEX);
}

void goDeepSleep() {
  Serial.println("Entering deep sleep mode...good dreams fellows...");
  ESP.deepSleep(0, WAKE_RFCAL); 
  yield();
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 

  int retry = 0;
  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      Serial.println("connected to MQTT Broker...");
    }
    else {
      delay(500);
      Serial.print(".");
    }
  }

  if (retry >= MAX_RETRY) {
    makeLedBlink(5, 100);
    goDeepSleep();
  }
}

float getVcc() {
  return (float)ESP.getVcc() * VCC_ADJ;  
}

void checkForUpdate() {

  HTTPClient httpClient;
  httpClient.setTimeout(2000);
  Serial.println("Calling : " + CHECK_FIRMWARE_VERSION_URL);
  httpClient.begin( CHECK_FIRMWARE_VERSION_URL );
  int httpCode = httpClient.GET();
  Serial.println("Http code received : "+String(httpCode));
  if( httpCode == 200 ) {  
    String result = httpClient.getString();
    Serial.print("Checking version : "+ result);
    JsonObject& parsed= JSONBuffer.parseObject(result);
    if (parsed.success()) {      
        int lastVersion = parsed["lastversion"];        
        Serial.println("Version courante : " + String(CURRENT_FIRMWARE_VERSION) + ", dernière version : " + String(lastVersion));
        if( lastVersion > CURRENT_FIRMWARE_VERSION ) {
          doOTAUpdate(lastVersion);
        }
    }
  }
  
  httpClient.end();  
}

void doOTAUpdate(int version) {

  t_httpUpdate_return ret  = ESPhttpUpdate.update(BASE_FIRMWARE_URL +  "/" + String(version));
  
  switch (ret) {
    case HTTP_UPDATE_FAILED:
      Serial.println("[update] Update failed.");      
      break;
    case HTTP_UPDATE_NO_UPDATES:
      Serial.println("[update] Update no Updates.");           
      break;
    case HTTP_UPDATE_OK:  
      Serial.println("[update] Update ok.");   
      break;
  }  
}

void loop() {

  //Handle MQTT connection
  client.loop();

  float vcc = getVcc();
  String mess = "SETGOTMAIL;" + String(vcc,2);

  Serial.println("Sending message : " + mess);
  mess.toCharArray(message_buff, mess.length()+1);
  
  //Send got mail notification
  client.publish(mailboxnotifier_topic, message_buff);

  yield();

  makeLedBlink(1,100);
  goDeepSleep();
}


