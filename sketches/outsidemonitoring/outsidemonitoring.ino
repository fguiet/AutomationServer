/****
 * Outside monitoring
 * 
 * F. Guiet 
 * Creation           : 20170225
 * Last modification  : 20190126 
 * 
 * Version            : 1.1
 * History            : 1.0 - First version
 *                      1.1 - Add deep sleep mode (so that temp sensor is not altered by ESP8266 self warm)
 *                      1.2 - Add debug_mode, update mqtt_topic, push message in JSon format
 *
 * Note               : Don't forget to change MQTT_MAX_PACKET_SIZE to 256 in PubSubClient.h (increase mqtt message length) - D:\Documents\guiet\Arduino\libraries
 */

//#define MQTT_MAX_PACKET_SIZE 256

#include <DallasTemperature.h>
#include <Wire.h>
#include <Adafruit_BMP085.h>
//Light Mqtt library
#include <PubSubClient.h>
//Wifi library
#include <ESP8266WiFi.h>
#include <ArduinoJson.h>

Adafruit_BMP085 bmp;

OneWire  oneWire(D4);
DallasTemperature DS18B20(&oneWire);

const int sclPin = D6;
const int sdaPin = D5;

//Mqtt settings
#define mqtt_server "192.168.1.25"
#define DEBUG 0
//#define mqtt_user ""
//#define mqtt_password ""
//#define mqtt_topic_bmp085 "guiet/garage/sensor/13"
//#define mqtt_topic_ds18b20 "guiet/outside/sensor/14"

struct Sensor {
    //String Address;
    String Name;    
    String SensorId;
    String Mqtt_topic;
};

#define FIRMWARE_VERSION "1.2"

// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

#define MQTT_CLIENT_ID "GarageOutsideSensor"
#define MAX_RETRY 50
long sleepInMinute = 1;

long previousMillis = 0;   
long interval = 60000; //One minute
char message_buff[200];

WiFiClient espClient;
PubSubClient client(espClient);

#define SENSORS_COUNT 2
Sensor sensors[SENSORS_COUNT];
void InitSensors() {
  
  sensors[0].Name = "BMP085 - Garage";
  sensors[0].SensorId = "13";
  sensors[0].Mqtt_topic = "guiet/garage/sensor/13";

  sensors[1].Name = "DS18B20 - Outside";
  sensors[1].SensorId = "14";
  sensors[1].Mqtt_topic = "guiet/outside/sensor/14";
}

void setup() {
  
  Serial.begin(115200);
  
  delay(100);

  pinMode(LED_BUILTIN, OUTPUT); 

  delay(100);

  InitSensors();
  
  makeLedBlink(2,500);
  
  debug_message("Starting outside monitoring...", true);
  
  Wire.begin(sdaPin, sclPin);

  if (!bmp.begin()) {
    debug_message("BMP180 / BMP085 introuvable ! Verifier le branchement ", true);    
    makeLedBlink(5,200);  
    goDeepSleep();       
  }

  client.setServer(mqtt_server, 1883); 
  
  delay(200);  

  connectToWifi();
  
  debug_message("ready!", true);
}

void debug_message(String message, bool doReturnLine) {
  if (DEBUG) {

    if (doReturnLine)
      Serial.println(message);
    else
      Serial.print(message);
  }
}

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(LED_BUILTIN, HIGH);
    delay(millisecond);
    digitalWrite(LED_BUILTIN, LOW);
    delay(millisecond);
  } 
}

void loop() {  

    if (WiFi.status() != WL_CONNECTED) {
      if (!connectToWifi())
        goDeepSleep();
    }  

    if (!client.connected()) {
      if (!reconnect()) {
        goDeepSleep();
      }
    }

    //Handle MQTT connection
    client.loop();

    delay(500);

    debug_message("Temperature garage = ", false);
    float temp = bmp.readTemperature();
    debug_message(String(temp,2), false);
    debug_message(" *C", true);
    
    debug_message("Pression = ", false);
    float pressure = bmp.readPressure();
    debug_message(String(pressure,2), false);
    debug_message(" Pa", true);
   
    debug_message("Altitude = ", false);
    float altitude = bmp.readAltitude();
    debug_message(String(altitude,2), false);
    debug_message(" metres", true);
  
    DS18B20.requestTemperatures();
    
    float outsideTemp = DS18B20.getTempCByIndex(0);
    debug_message("Temp DS18B20 : " + String(outsideTemp,2), true);

    String mess = ConvertToJSon(String(temp,2), String(pressure,2), String(altitude,2));
    debug_message("JSON Sensor BMP085 : " + mess + ", topic : " +sensors[0].Mqtt_topic, true);
    mess.toCharArray(message_buff, mess.length()+1);
    
    client.publish(sensors[0].Mqtt_topic.c_str(),message_buff);

    delay(100);

    mess = ConvertToJSon1(String(outsideTemp,2));
    mess.toCharArray(message_buff, mess.length()+1);   
    debug_message("JSON Sensor DS18B20 : " + mess, true);
    client.publish(sensors[1].Mqtt_topic.c_str(),message_buff);
           
    makeLedBlink(2,100);

    delay(500);
    //yield();
  
    //Deep sleep...ZZzzzZZzzz
    goDeepSleep();
}

String ConvertToJSon1(String temperature) {
    //Create JSon object
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.createObject();
    
    root["sensorid"] = sensors[1].SensorId;
    root["name"] = sensors[1].Name;
    root["firmware"]  = FIRMWARE_VERSION;
    root["temperature"] = temperature;
   
    String result;
    root.printTo(result);

    return result;
}

String ConvertToJSon(String temperature, String pressure, String altitude) {
    //Create JSon object
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.createObject();
    
    root["sensorid"] = sensors[0].SensorId;
    root["name"] = sensors[0].Name;
    root["firmware"]  = FIRMWARE_VERSION;
    root["temperature"] = temperature;
    root["altitude"] = altitude;
    root["pressure"] = pressure;

    String result;
    root.printTo(result);

    return result;
}

void disconnectMqtt() {
  debug_message("Disconnecting from mqtt...", true);
  client.disconnect();
}

void disconnectWifi() {
  debug_message("Disconnecting from wifi...", true);
  WiFi.disconnect();
}

boolean reconnect() {

  int retry = 0;
  // Loop until we're reconnected
  while (!client.connected() && retry < MAX_RETRY) {
    debug_message("Attempting MQTT connection...", true);
    
    if (client.connect(MQTT_CLIENT_ID)) {
      debug_message("connected to MQTT Broker...", true);
    } else {
      retry++;
      // Wait 5 seconds before retrying
      delay(500);
      //yield();
    }
  }

  if (retry >= MAX_RETRY) {
    debug_message("MQTT connection failed...", true);  
    return false;
    //goDeepSleep();
  }

  return true;
}

boolean connectToWifi() {

  WiFi.forceSleepWake();
  WiFi.mode(WIFI_STA);
  
  int retry = 0;
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    retry++;
    delay(500);
    debug_message(".", false);
  }

  if (WiFi.status() == WL_CONNECTED) {  
     debug_message("WiFi connected", true);  
     // Print the IP address
     if (DEBUG) {
      Serial.println(WiFi.localIP());
     }
     
     return true;
  } else {
    debug_message("WiFi connection failed...", true);   
    return false;
  }  
}

void goDeepSleep() {
  //Disconnect properly

  if (client.connected()) {
    disconnectMqtt();  
  }

  if (WiFi.status() == WL_CONNECTED) {
    disconnectWifi();  
  }
  
  delay(500);
  //yield();

  debug_message("Entering deep sleep mode...good dreams fellows...ZzzzzZzzZZZZzzz", true);
  ESP.deepSleep(sleepInMinute * 60 * 1000000);
  delay(100);
  //yield();
}
