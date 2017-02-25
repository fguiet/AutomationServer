/****
 * Outside monitoring
 * 
 * F. Guiet 
 * Creation           : 20170225
 * Last modification  : 20170225 
 * 
 * Version            : 1.0
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
  }

  client.setServer(mqtt_server, 1883); 
  
  delay(200);  
}

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(LED_BUILTIN, HIGH);
    delay(millisecond);
    digitalWrite(LED_BUILTIN, LOW);
    delay(millisecond);
  } 
}

void connectToWifi() {

  WiFi.forceSleepWake();
  WiFi.mode(WIFI_STA);
  
  int retry = 0;
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED && retry < 100) {
    retry++;
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {  
     Serial.println("");
     Serial.println("WiFi connected");  
     // Print the IP address
     Serial.println(WiFi.localIP());     
  } else {
    Serial.println("WiFi connection failed...");  
    makeLedBlink(5, 200);    
  }  
}

void loop() {  

  unsigned long currentMillis = millis();
  
  if(currentMillis - previousMillis > interval) {

    connectToWifi();

    // put your main code here, to run repeatedly:
    if (!client.connected()) {
      reconnect();     
    }

    //Handle MQTT connection
    client.loop();

    previousMillis = currentMillis;

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

    WiFi.disconnect();
  }
}

void reconnect() {

  int retry = 0;
  // Loop until we're reconnected
  while (!client.connected() && retry < 10) {
    Serial.print("Attempting MQTT connection...");
    
    if (client.connect("Wemos_OutsideNotifier")) {
      Serial.println("connected to MQTT Broker...");
    } else {
      retry++;
      // Wait 5 seconds before retrying
      delay(500);      
    }
  }

  if (retry >= 10) {
    Serial.println("MQTT connection failed...");  
    makeLedBlink(5, 200);    
  }
}

