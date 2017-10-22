/****
 * Rollershutter manager
 * 
 * F. Guiet 
 * Creation           : 20171022
 * Last modification  : 20171022
 * 
 * Version            : 1
 * 
 * History            : 1.0 - Initial version
 *                      
 */

#include <Ethernet.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <SPI.h>
#include <SoftwareReset.h>
#include <SimpleTimer.h>

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE);
char message_buff[100];

// Update these with values suitable for your network.
byte mac[]    = {  0xDE, 0xED, 0xBA, 0xFE, 0xFE, 0xED };
IPAddress ip(192, 168, 1, 20);
IPAddress gateway_ip(192, 168, 1, 1); // set gateway to match your network
IPAddress dns_ip(192, 168, 1, 1); // set gateway to match your network
IPAddress subnet_mask(255, 255, 255, 0);


//Mqtt broker IP
IPAddress mqtt_server(192, 168, 1, 25);

EthernetClient ethClient;
PubSubClient client(ethClient);

String SENSORID= "9";   
String MQTT_CLIENT_ID = "BasementSensor" + SENSORID;
#define MAX_RETRY 50
#define MQTT_TOPIC "/guiet/cave/temphumi"

#define RELAY_PIN 3
int humidityMax = 80;

SimpleTimer pushDataTimer;

void setup() {
    
  Serial.begin(115200);

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);

  dht.begin();

  Ethernet.begin(mac, ip, dns_ip, gateway_ip, subnet_mask);
  
  // Allow the hardware to sort itself out
  delay(1500);

  connectToMqtt();

  pushDataTimer.setInterval(60000, pushData);
  
  Serial.println("Ready...");
}

void loop() {

  //In case of Wifi deconnection...just reboot it is better...
  if (!client.connected()) {
      softwareReset(STANDARD);
  }

  client.loop();

  pushDataTimer.run();
}

String fanManagement(float humidity) {

  if (humidity > humidityMax) {
    humidityMax=80;
    digitalWrite(RELAY_PIN, HIGH);
    return "ON";
  }
  else {
    humidityMax=82;
    digitalWrite(RELAY_PIN, LOW);
    return "OFF";
  }
}

void pushData() {
   float h = dht.readHumidity();
   float t = dht.readTemperature();

   if (isnan(t) || isnan(h)) {
      Serial.println("Failed to read from DHT");
      //fanManagement(h);        
   }
   else {
      Serial.println("Temp : "+String(t,2));
      Serial.println("Humi : "+String(h,2));

      String fanStatus = fanManagement(h);
      
      String mess = "SETCAVEINFO;"+String(t,2)+";"+String(h,2)+";"+fanStatus;
      mess.toCharArray(message_buff, mess.length()+1);
      client.publish(MQTT_TOPIC,message_buff);
   }  
}

void connectToMqtt() {
  
  client.setServer(mqtt_server, 1883);   

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
    softwareReset(STANDARD);
  } 
}


