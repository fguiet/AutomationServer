
/**** 
 * Chritmas Lights On/Off
 * 
 * F. Guiet 
 * Creation           : 20171119
 * Last modification  : 20171119
 * 
 * Version            : 1.0
 * 
 * History            : 1.0 - Creation
 *                      
 * Note               : OTA only work correcly only and only if a hard reset is done AFTER serial port upload otherwise ESP will fail to start up on when OTA update occurs
 *                      https://github.com/esp8266/Arduino/issues/1782                     
 *                      https://github.com/esp8266/Arduino/issues/1017
 *                      
 */

#include <ArduinoJson.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266httpUpdate.h>
#include <ESP8266WiFi.h>  
#include <PubSubClient.h>
#include <SimpleTimer.h>

/**** VARIABLES ***/
DynamicJsonBuffer JSONBuffer;
WiFiClient espClient;
PubSubClient client(espClient);
char message_buff[100];
#define RELAY_PIN D2
/**** END VARIABLES ***/

/**** DEFINE ***/
String SENSORID =  "10"; //XmasLights
#define MAX_RETRY 50
String MQTT_CLIENT_ID = "XmasLightsSensor";
#define MQTT_SERVER "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""
#define MQTT_TOPIC "/guiet/outside/xmaslights"
#define sub_topic1 "/guiet/automationserver/xmaslights"
const int CURRENT_FIRMWARE_VERSION = 1;
String CHECK_FIRMWARE_VERSION_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getversion/" + SENSORID;
String BASE_FIRMWARE_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getfirmware/" + SENSORID;
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";
IPAddress ip_wemos (192,168,1,43); //Xmas lights
IPAddress gateway_ip (192,168,1,1);
IPAddress subnet_mask(255, 255, 255,0);

/**** END DEFINE ***/ 

SimpleTimer checkForUpdateTimer;

void setup() {

   //Initialize Serial
  Serial.begin(115200);

  Serial.println("Initializing...");
  
  //Connecting to WiFi
  connectToWifi();

  //Check for firmware update
  checkForUpdate();

  //No update necessary
  //Connect to Mqtt
  connectToMqtt();  

  checkForUpdateTimer.setInterval(60000, checkForUpdate);

  pinMode(RELAY_PIN,OUTPUT);
  digitalWrite(RELAY_PIN, LOW); 

  Serial.println("Ready...");
}

void loop() {
  
  //In case of Wifi deconnection...just reboot it is better...
  if (WiFi.status() != WL_CONNECTED || !client.connected()) {
      ESP.restart();
  }

  client.loop();
  
  checkForUpdateTimer.run();
}


void activateRelay(bool on) {

  if (on) {
    digitalWrite(RELAY_PIN, HIGH); 
  }
  else {
    digitalWrite(RELAY_PIN, LOW); 
  }
  
}

String getValue(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length()-1;

  for(int i=0; i<=maxIndex && found<=index; i++){
    if(data.charAt(i)==separator || i==maxIndex){
        found++;
        strIndex[0] = strIndex[1]+1;
        strIndex[1] = (i == maxIndex) ? i+1 : i;
    }
  }

  return found>index ? data.substring(strIndex[0], strIndex[1]) : "";
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {

  char message_buff[100];

  Serial.println("Message recu =>  topic: " + String(topic));

  int i = 0;
  for(i=0; i<length; i++) {
    message_buff[i] = payload[i];
  }
  message_buff[i] = '\0';
  
  String receivedPayload = String(message_buff);

  Serial.println("Payload: " + receivedPayload);

  String action = getValue(receivedPayload, ';', 0);
  String id = getValue(receivedPayload, ';', 1);
  
  if (id == SENSORID) {
    if (action == "SETXMASLIGHTSON")  {
      activateRelay(true);
    } 
    else {
      if (action == "SETXMASLIGHTSOFF")  {
        activateRelay(false);
      }
      else {
        activateRelay(false);
      }
    }        
  }
}

void connectToWifi() 
{
  byte mac[6];

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

  if (retry >= MAX_RETRY)
    ESP.restart();
  
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

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 
  client.setCallback(mqttCallback);

  int retry = 0;
  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID.c_str())) {
      Serial.println("connected to MQTT Broker...");
    }
    else {
      delay(500);
      Serial.print(".");
    }
  }

  if (retry >= MAX_RETRY) {
    ESP.restart();
  } else {
    client.subscribe(sub_topic1);    
  }
}

