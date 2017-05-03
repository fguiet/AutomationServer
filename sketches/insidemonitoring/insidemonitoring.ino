/**** 
 * Mailbox Notifier
 * 
 * F. Guiet 
 * Creation           : 20170218
 * Last modification  : 20170503
 * 
 * Version            : 1.1
 */

#include <DHT.h>

//Light Mqtt library
#include <PubSubClient.h>

//Wifi library
#include <ESP8266WiFi.h>

#define DHTTYPE DHT22
#define DHTPIN D2
DHT dht(DHTPIN, DHTTYPE);

//#define SENSORID 5 //Parents
//#define SENSORID 4 //Manon
//#define SENSORID 3 //Nohé
//#define SENSORID 2 //Salon
#define SENSORID 1 //Bureau

//#define MQTT_CLIENT_ID "ParentsSensor"
//#define MQTT_CLIENT_ID "ManonSensor"
//#define MQTT_CLIENT_ID "NoheSensor"
//#define MQTT_CLIENT_ID "SalonSensor"
#define MQTT_CLIENT_ID "BureauSensor"

IPAddress ip_wemos(192,  168,   1,   35); //bureau
//IPAddress ip_wemos(192,  168,   1,   36); //Salon
//IPAddress ip_wemos(192,  168,   1,   37); //Manon
//IPAddress ip_wemos(192,  168,   1,   38); //Parents
//IPAddress ip_wemos(192,  168,   1,   39); //Nohe

//Mqtt settings

#define MQTT_SERVER "192.168.1.25"
//#define mqtt_user ""
//#define mqtt_password ""
#define MQTT_TOPIC "/guiet/inside/sensor"

const int SLEEP_TIME_SECONDS = 60;
#define MAX_RETRY 50

// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

IPAddress gateway_ip ( 192,  168,   1,   1);
IPAddress subnet_mask(255, 255, 255,   0);

WiFiClient espClient;
PubSubClient client(espClient);

char message_buff[100];

void setup() {

  Serial.begin(115200); 

  delay(1000);

  Serial.println("Initializing DHT...");

  dht.begin();

  Serial.println("Initializing MQTT...");
  client.setServer(MQTT_SERVER, 1883); 
  
  connectToWifi();
  
  Serial.println("ready!");

}

void loop() {

  if (WiFi.status() != WL_CONNECTED) {
    if (!connectToWifi())
      goDeepSleep();      
      return;
  }  

  if (!client.connected()) {
    if (!reconnect()) {
      goDeepSleep();      
      return;
    }
  }

  //Handle MQTT connection
  client.loop();

  //delay(1000);
  
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  //https://gist.github.com/tzapu/e6e70b4c094be618b714050a252309a3
  //Read twice to get accurate result!!
  delay(2100);

  h = dht.readHumidity();
  t = dht.readTemperature();

  if (isnan(t) || isnan(h)) 
  {
    Serial.println("Failed to read from DHT");        
    //goDeepSleep();
  }
  else {

    Serial.println("Temp : "+String(t,2));
    Serial.println("Humi : "+String(h,2));

    String mess = "SETINSIDEINFO;"+String(SENSORID)+";"+String(t,2)+";"+String(h,2);
    mess.toCharArray(message_buff, mess.length()+1);
    client.publish(MQTT_TOPIC,message_buff);
  }

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

void goDeepSleep() {
  //Disconnect properly

  if (client.connected()) {
    disconnectMqtt();  
  }

  if (WiFi.status() == WL_CONNECTED) {
    disconnectWifi();  
  }

  /*rst_info *rsti;
  rsti = ESP.getResetInfoPtr();
  Serial.println( String( "ResetInfo.reason = " ) + rsti->reason );*/

  Serial.println("Entering deep sleep mode...good dreams fellows...ZzzzzZzzZZZZzzz");
  ESP.deepSleep(SLEEP_TIME_SECONDS * 1000000);
  delay(200); //Recommanded
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
      Serial.print(".");
      //yield();
    }
  }

  if (retry >= MAX_RETRY) {
    Serial.println("MQTT connection failed...");  
    return false;
  }

  return true;
}

boolean connectToWifi() {

  WiFi.forceSleepWake();
  WiFi.mode(WIFI_STA);
  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  
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

