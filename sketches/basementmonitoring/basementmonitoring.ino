/****
 * Mailbox Notifier
 * 
 * F. Guiet 
 * Creation           : 20170218
 * Last modification  : 20170422
 * 
 * Version            : 1.1
 */

#include <DHT.h>
#include <SPI.h>
#include <Ethernet.h>
#include <PubSubClient.h>

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE);

#define RELAY_PIN 3

int humidityMax = 80;
long previousMillis = 0;   
long interval = 60000; //One minute
char message_buff[100];

// Update these with values suitable for your network.
byte mac[]    = {  0xDE, 0xED, 0xBA, 0xFE, 0xFE, 0xED };
IPAddress ip(192, 168, 1, 20);

//Mqtt broker IP
IPAddress server(192, 168, 1, 25);

/*void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i=0;i<length;i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}*/

EthernetClient ethClient;
PubSubClient client(ethClient);

#define MQTT_CLIENT_ID "BasementSensor"
#define MAX_RETRY 50
#define MQTT_TOPIC "/guiet/cave/temphumi"

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
  }

}

void setup()
{
  Serial.begin(9600);

  dht.begin();

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW);
  
  client.setServer(server, 1883);
  //client.setCallback(callback);

  Ethernet.begin(mac, ip);
  // Allow the hardware to sort itself out
  delay(1500);
}

void loop()
{ 
 
  unsigned long currentMillis = millis();
  
  if(currentMillis - previousMillis > interval) {     

     previousMillis = currentMillis;

     if (!client.connected()) {
       if (!reconnect()) {
        return;
       }
     }
     
     client.loop();
     
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

     client.disconnect();
  }
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



