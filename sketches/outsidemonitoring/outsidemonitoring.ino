/****
 * Outside monitoring
 * 
 * F. Guiet 
 * Creation           : 20170225
 * Last modification  : 20170410 
 * 
 * Version            : 1.1
 * History            : 1.0 - First version
 *                      1.1 - Add deep sleep mode (so that temp sensor is not altered by ESP8266 self warm
 */

#include <DallasTemperature.h>
#include <Wire.h>
#include <Adafruit_BMP085.h>
//Light Mqtt library
#include <PubSubClient.h>
//Wifi library
#include <ESP8266WiFi.h>

Adafruit_BMP085 bmp;

OneWire  oneWire(D4);
DallasTemperature DS18B20(&oneWire);

const int sclPin = D6;
const int sdaPin = D5;

//Mqtt settings
#define mqtt_server "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""
#define mqtt_topic "/guiet/outside/sensorsinfo"

// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

#define MQTT_CLIENT_ID "GarageSensor"
#define MAX_RETRY 50
long sleepInMinute = 1;

long previousMillis = 0;   
long interval = 60000; //One minute
char message_buff[100];

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  
  Serial.begin(115200);
  
  //delay(200);

  pinMode(LED_BUILTIN, OUTPUT); 

  yield();
  delay(200);
  
  makeLedBlink(2,500);
  
  Serial.println("Starting outside monitoring...");
  
  Wire.begin(sdaPin, sclPin);
  
  if (!bmp.begin()) {
    Serial.println("BMP180 / BMP085 introuvable ! Verifier le branchement ");    
    makeLedBlink(5,200);  
    goDeepSleep();       
  }

  client.setServer(mqtt_server, 1883); 
  
  delay(200);  

  connectToWifi();
  
  Serial.println("ready!");
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

 // unsigned long currentMillis = millis();
  
  //if(currentMillis - previousMillis > interval) {

   // connectToWifi();

    // put your main code here, to run repeatedly:
    //if (!client.connected()) {
    //  reconnect();     
   // }

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
    yield();

    //previousMillis = currentMillis;

    Serial.print("Temperature garage = ");
    float temp = bmp.readTemperature();
    Serial.print(temp);
    Serial.print(" *C");
    
    Serial.print(" | Pression = ");
    float pressure = bmp.readPressure();
    Serial.print(pressure);
    Serial.print(" Pa");
   
    Serial.print(" | Altitude = ");
    float altitude = bmp.readAltitude();
    Serial.print(altitude);
    Serial.println(" metres");
  
    DS18B20.requestTemperatures();
    
    float outsideTemp = DS18B20.getTempCByIndex(0);
    Serial.println("Temp DS18B20 : " + String(outsideTemp,2));

    String mess = "SETOUTSIDEINFO;"+String(temp,2)+";"+String(pressure,2)+";"+String(altitude,2)+";"+String(outsideTemp,2);
    mess.toCharArray(message_buff, mess.length()+1);
    client.publish(mqtt_topic,message_buff);       
    makeLedBlink(2,100);

    delay(500);
    yield();
  
    //Deep sleep...ZZzzzZZzzz
    goDeepSleep();
}

void disconnectMqtt() {
  Serial.println("Disconnecting from mqtt...");
  client.disconnect();
}

void disconnectWifi() {
  Serial.println("Disconnecting from wifi...");
  WiFi.disconnect();
}

boolean reconnect() {

  int retry = 0;
  // Loop until we're reconnected
  while (!client.connected() && retry < MAX_RETRY) {
    Serial.print("Attempting MQTT connection...");
    
    if (client.connect(MQTT_CLIENT_ID)) {
      Serial.println("connected to MQTT Broker...");
    } else {
      retry++;
      // Wait 5 seconds before retrying
      delay(500);
      yield();
    }
  }

  if (retry >= MAX_RETRY) {
    Serial.println("MQTT connection failed...");  
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
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {  
     Serial.println("");
     Serial.println("WiFi connected");  
     // Print the IP address
     Serial.println(WiFi.localIP());
     return true;
  } else {
    Serial.println("WiFi connection failed...");   
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
  yield();

  Serial.println("Entering deep sleep mode...good dreams fellows...ZzzzzZzzZZZZzzz");
  ESP.deepSleep(sleepInMinute * 60 * 1000000);
  yield();
}

