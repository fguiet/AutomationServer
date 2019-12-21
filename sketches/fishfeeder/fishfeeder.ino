
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <Stepper.h>

#define DEBUG 1
#define MAX_RETRY 50
#define MQTT_SERVER "192.168.1.25"
#define MQTT_CLIENT_ID "FishFeederMqttClient"
#define MQTT_TOPIC "/guiet/fishfeeder"

const int SERIAL_BAUD = 9600;

const char* ssid = "DUMBLEDORE";
const char* password = "frederic";
const int stepsPerRevolution = 2048;

Stepper myStepper(stepsPerRevolution, D0, D6, D5, D7);
WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
   //Initialize Serial
  Serial.begin(SERIAL_BAUD);

  pinMode(D0,OUTPUT);
  pinMode(D6,OUTPUT);
  pinMode(D5,OUTPUT);
  pinMode(D7,OUTPUT);
  pinMode(BUILTIN_LED, OUTPUT);

  // set the speed at 10 rpm:
  myStepper.setSpeed(10);

  digitalWrite(BUILTIN_LED, LOW);
  connectToWifi();
  connectToMqtt();  
   digitalWrite(BUILTIN_LED, HIGH);

  debug_message("Ready", true);

}

void debug_message(String message, bool doReturnLine) {
  if (DEBUG) {

    if (doReturnLine)
      Serial.println(message);
    else
      Serial.print(message);
  }
}

void loop() {
  
  if (WiFi.status() != WL_CONNECTED || !client.connected()) {
      ESP.restart();
  }

   client.loop();

  //digitalWrite(BUILTIN_LED, LOW);
  //delay(100);
  //digitalWrite(BUILTIN_LED, HIGH);
  delay(100);
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {

  char message_buff[100];

  debug_message("Message recu =>  topic: " + String(topic),true);  

  int i = 0;
  for(i=0; i<length; i++) {
    message_buff[i] = payload[i];
  }
  message_buff[i] = '\0';
  
  String receivedPayload = String(message_buff);

  debug_message("Payload: " + receivedPayload,true);  
  //Serial.println("Payload: " + receivedPayload);

  if (receivedPayload == "FEEDTHEFISH") {
    feedTheFish();  
  }
}

void feedTheFish() {
  myStepper.step(132);
  
  //This kills the power to the stepper to save energy and to keep it from heating up.
  digitalWrite(D6, LOW);
  digitalWrite(D7, LOW);
  digitalWrite(D5, LOW);
  digitalWrite(D0, LOW);  
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 
  client.setCallback(mqttCallback);

  int retry = 0;

  debug_message("Attempting MQTT connection...", true);
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      debug_message("connected to MQTT Broker...", false);
    }
    else {
      delay(500);
      debug_message(".", false);
      retry++;
    }

    if (retry >= MAX_RETRY) {
      ESP.restart();
    }
    else {
      client.subscribe(MQTT_TOPIC);      
    }
  }   
}

void connectToWifi() 
{
  byte mac[6];

  WiFi.disconnect();

  debug_message("Connecting to WiFi...", true);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);

    debug_message(".", false);
    retry++;  

    if (retry >= MAX_RETRY)
      ESP.restart();
  }

  if (DEBUG) {
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
}
