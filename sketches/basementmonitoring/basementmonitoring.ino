/****
 * Mailbox Notifier
 * 
 * F. Guiet 
 * Creation           : 20170218
 * Last modification  : 20170218 
 * 
 * Version            : 1.0
 */

#include <DHT.h>
#include <SPI.h>
#include <Ethernet.h>
#include <PubSubClient.h>

#define DHTPIN 2     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE);

#define RELAY_PIN 3

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

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    if (client.connect("arduinoClient")) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      //client.publish("/guiet/cave/temphumi","hello world");
      // ... and resubscribe
      //client.subscribe("inTopic");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
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
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  unsigned long currentMillis = millis();
  
  if(currentMillis - previousMillis > interval) {

     previousMillis = currentMillis;
     
     float h = dht.readHumidity();
     float t = dht.readTemperature();

     if (isnan(t) || isnan(h)) {
        Serial.println("Failed to read from DHT");
        fanManagement(h);        
     }
     else {
        Serial.println("Temp : "+String(t,2));
        Serial.println("Humi : "+String(h,2));

        String fanStatus = fanManagement(h);
        
        String mess = "SETCAVEINFO;"+String(t,2)+";"+String(h,2)+";"+fanStatus;
        mess.toCharArray(message_buff, mess.length()+1);
        client.publish("/guiet/cave/temphumi",message_buff);
     }  
  }
}

String fanManagement(float humidity) {
  if (isnan(humidity)) {
    digitalWrite(RELAY_PIN, LOW);
    return "OFF";
  }

  if (humidity > 80) {
    digitalWrite(RELAY_PIN, HIGH);
    return "ON";
  }
  else {
    digitalWrite(RELAY_PIN, LOW);
    return "OFF";
  }
}



