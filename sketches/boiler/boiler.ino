/**** 
 * Boiler Temp monitoring
 * 
 * F. Guiet 
 * Creation           : 20180430
 * Last modification  : 
 * 
 * Version            : 1.0
 * 
 * History            : 1  - Creation
*                      
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
#include <OneWire.h> //Librairie du bus OneWire
#include <DallasTemperature.h> //Librairie du capteur 


#include <PubSubClient.h>

/**** VARIABLES ***/
DynamicJsonBuffer JSONBuffer;

WiFiClient espClient;
PubSubClient client(espClient);
char message_buff[100];

OneWire oneWire(D2); //Bus One Wire sur la pin 2 de l'arduino
DallasTemperature sensor(&oneWire); //Utilistion du bus Onewire pour les capteurs
DeviceAddress sensorDeviceAddress; //Vérifie la compatibilité des capteurs avec la librairie

/**** END VARIABLES ***/

/**** DEFINE ***/
const int SLEEP_TIME_SECONDS = 60;
String SENSORID =  "11"; //Boiler
#define MAX_RETRY 50
#define MQTT_CLIENT_ID "BoilerSensor"
#define MQTT_SERVER "192.168.1.25"
#define MQTT_TOPIC "/guiet/boiler/temp"
const int CURRENT_FIRMWARE_VERSION = 4;
String CHECK_FIRMWARE_VERSION_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getversion/" + SENSORID;
String BASE_FIRMWARE_URL = "http://192.168.1.25:8510/automationserver-webapp/api/firmware/getfirmware/" + SENSORID;
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

IPAddress ip_wemos (192,168,1,42); 
IPAddress gateway_ip ( 192,168,1,1);
IPAddress subnet_mask(255, 255, 255,0);

/**** END DEFINE ***/ 


void setup() {
  //Initialize Serial
  Serial.begin(115200);  
  
  //Connecting to WiFi
  connectToWifi();

  //Check for firmware update
  checkForUpdate();

  //Init Sensor
  sensor.begin(); //Activation des capteurs
  sensor.getAddress(sensorDeviceAddress, 0); //Demande l'adresse du capteur à l'index 0 du bus
  sensor.setResolution(sensorDeviceAddress, 12); //Possible resolution : 9,10,11,12

  //No update necessary
  //Connect to Mqtt
  connectToMqtt();   
}

void loop() {

  ///Wait a little before beginning
  delay(2000);

  //Handle MQTT connection
  client.loop();   
  
  float t = getTemperature();
  
  //Useful?
  //Read twice to get accurate result!!
  delay(2000);

  t = getTemperature();

  String mess = "SETBOILERINFO;"+String(SENSORID)+";"+String(t,2)+";"+String(CURRENT_FIRMWARE_VERSION);
  mess.toCharArray(message_buff, mess.length()+1);
  client.publish(MQTT_TOPIC,message_buff);

  //Deep sleep...ZZzzzZZzzz
  goDeepSleep(); 


}

float getTemperature() {
  sensor.requestTemperatures(); //Demande la température aux capteurs
  return sensor.getTempCByIndex(0);
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

  int retry = 0;
  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      Serial.println("connected to MQTT Broker...");
    }
    else {
      retry++;
      delay(500);
      Serial.print(".");
    }

    
  }

  if (retry >= MAX_RETRY) {
    ESP.restart();
  }
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

void disconnectMqtt() {
  Serial.println("Disconnecting from mqtt...");
  client.disconnect();
}

void disconnectWifi() {
  Serial.println("Disconnecting from wifi...");
  WiFi.disconnect();
}

void goDeepSleep() {
  //Disconnect pro
  if (client.connected()) {
    disconnectMqtt();  
  }

  if (WiFi.status() == WL_CONNECTED) {
    disconnectWifi();  
  }  

  Serial.println("Entering deep sleep mode...good dreams fellows...ZzzzzZzzZZZZzzz");
  ESP.deepSleep(SLEEP_TIME_SECONDS * 1000000);
  delay(200); //Recommanded
}
