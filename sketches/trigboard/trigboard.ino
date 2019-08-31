/****
 * TrigBoard
 * 
 * F. Guiet 
 * Creation           : 20190204
 * Last modification  : 20190420 
 * 
 * Version            : 1.0
 * History            : 1.0 - First version
 *                      1.1 - Add StillOnCheck
 *                      
 * Note               : Credits go to Kevin Darrah who inspired me to build this trigboard (https://www.kevindarrah.com/wiki/index.php?title=TrigBoard)
 *                      ESP Board I am using ESP-12-E
*/

//Light Mqtt library
#include <PubSubClient.h>
//Wifi library
#include <ESP8266WiFi.h>
#include <ArduinoJson.h>

//Mqtt settings
#define MQTT_SERVER "192.168.1.25"
#define DEBUG 1

#define LED_PIN 2 //ESP-12-E led pin
#define DONE_PIN 12
#define STILLOPEN_PIN 13 //To check whether reed switch is still open 
#define EXTWAKE_PIN 5 //To check whether it is an external wake up or a tpl5111 timer wake up
#define ADC_PIN 0 //To read battery voltage

// WiFi settings
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

#define SMS_TOPIC  "guiet/automationserver/smsservice"
#define MQTT_CLIENT_ID "TrigBoardGarageDoorSensor"
#define MAX_RETRY 100
#define FIRMWARE_VERSION "1.1"

WiFiClient espClient;
PubSubClient client(espClient);

struct Sensor {
    //String Address;
    String Name;    
    String SensorId;
    String Mqtt_topic;
    String External_wakeup;
};

#define SENSORS_COUNT 1
Sensor sensors[SENSORS_COUNT];

char message_buff[200];

void InitSensors(bool externalWakeUp) {
  
  sensors[0].Name = "Trigboard - Garage door";
  sensors[0].SensorId = "18";
  sensors[0].Mqtt_topic = "guiet/garage/sensor/18";

  if (externalWakeUp)
     sensors[0].External_wakeup = "oui";
  else
     sensors[0].External_wakeup = "non";
}

/*
 * EXTWAKE_PIN is HIGH when ESP is wake up by TPL5111 
 */
bool isExternalWakeUp() {
  if (digitalRead(EXTWAKE_PIN) == HIGH) {
    return false; 
  }
  else {
    return true;
  }
}

void SendStillOpenMessage() {
  if (WiFi.status() != WL_CONNECTED) {
    if (!connectToWifi())
      weAreDone();
  }  

  if (!client.connected()) {
    if (!connectToMqtt()) {
      weAreDone();
    }
  }

  String mess = ConvertToJSonShortTestMsg("Attention, la porte du garage est toujours ouverte.");
  debug_message("JSON Sensor : " + mess + ", topic : " +SMS_TOPIC, true);
  mess.toCharArray(message_buff, mess.length()+1);
    
  client.publish(SMS_TOPIC,message_buff);

  disconnectMqtt();
  delay(100);
  disconnectWifi();
  delay(100);
  
}

/*
 * STILLON_PIN is HIGH when reedswitch is opened 
 */
bool isStillOpen() {
  if (digitalRead(STILLOPEN_PIN) == HIGH) {
    return true; 
  }
  else {
    return false;
  }
}

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(LED_PIN, HIGH);
    delay(millisecond);
    digitalWrite(LED_PIN, LOW);
    delay(millisecond);
  } 
}

/* The DONE pin is driven by a μC to signal that the μC is working properly. The TPL5111 recognizes a valid
DONE signal as a low to high transition. */
void weAreDone() {

  debug_message("Bye bye dude...", true);
  
  for (int i=0;i<10;i++) {
    digitalWrite(DONE_PIN, HIGH);
    delay(100);
    digitalWrite(DONE_PIN, LOW);
    delay(100);
  }
}

float readVoltage() {

  //R1 = 33kOhm
  //R2 = 7.5kOhm

  float sensorValue = 0.0f;
  float R1 = 32800;
  float R2 =  7460;
  float vmes = 0.0f;
  float vin = 0.f;
    
  sensorValue = analogRead(ADC_PIN);

  debug_message("Analog value : " + String(sensorValue,2), true);

  //ADC between 0 and 1v...maybe ajusted a little
  //1.1v is a little lower than expected...
  vmes = (sensorValue) / 1024;

  //Calcul issue du pont diviseur
  vin = vmes / (R2 / (R1 + R2));

  debug_message("Battery voltage : " + String(vin,2), true);

  return vin;  
}

void setup() {

  //Initialise PINS
  pinMode(EXTWAKE_PIN, INPUT);
  pinMode(STILLOPEN_PIN, INPUT_PULLUP);
  pinMode(DONE_PIN, OUTPUT);
  pinMode(LED_PIN, OUTPUT);
  pinMode(ADC_PIN, OUTPUT);

  //Turn off LED
  digitalWrite(LED_PIN, HIGH);

  //For debug purpose
  Serial.begin(115200);
  delay(100);

  bool externalWakeUp = true;

  //External or TPL5111 wake up?
  if (!isExternalWakeUp()) {

    debug_message("TPL5111 woke me up! leave me alone...", true);
    
    makeLedBlink(3,200);

    externalWakeUp = false;    

    //Check if reed switch is open or not
    if (isStillOpen()) {
      debug_message("reedswitch is open", true);
      InitSensors(externalWakeUp);
      SendStillOpenMessage();
    }
    else {
      debug_message("reedswitch is NOT open", true);
    }

    //Go to sleep immediatly
    weAreDone();
    return;
  }
  else {
    
    debug_message("Something in the maibox, time to send a message!...", true);
    InitSensors(externalWakeUp);
    
  }
}

void disconnectMqtt() {
  debug_message("Disconnecting from mqtt...", true);
  client.disconnect();
}

void disconnectWifi() {
  debug_message("Disconnecting from wifi...", true);
  WiFi.disconnect();
}

void debug_message(String message, bool doReturnLine) {
  if (DEBUG) {
    if (doReturnLine)
      Serial.println(message);
    else
      Serial.print(message);
  }
}

boolean connectToMqtt() {

   client.setServer(MQTT_SERVER, 1883); 

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

  // WiFi.forceSleepWake();
  WiFi.mode(WIFI_STA);
  WiFi.setOutputPower(20.5); // this sets wifi to highest power 
  
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

String ConvertToJSonShortTestMsg(String message) {
    //Create JSon object
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.createObject();

    //Garage door still open guid
    String guid ="cce5d7f1-7aeb-443b-9b6d-c41db55da832";
    
    root["messageid"] = guid;
    root["messagetext"] = message;
           
    String result;
    root.printTo(result);

    return result;
}

String ConvertToJSon(String battery) {
    //Create JSon object
    DynamicJsonBuffer  jsonBuffer(200);
    JsonObject& root = jsonBuffer.createObject();
    
    root["sensorid"] = sensors[0].SensorId;
    root["name"] = sensors[0].Name;
    root["firmware"]  = FIRMWARE_VERSION;
    root["battery"] = battery;
    root["externalwakeup"] = sensors[0].External_wakeup; 
       
    String result;
    root.printTo(result);

    return result;
}

void loop() {
  //Turn on LED...
  digitalWrite(LED_PIN, LOW);

  if (WiFi.status() != WL_CONNECTED) {
    if (!connectToWifi())
      weAreDone();
  }  

  if (!client.connected()) {
    if (!connectToMqtt()) {
      weAreDone();
    }
  }

  //Read battery voltage
  float vin = readVoltage();

  String mess = ConvertToJSonShortTestMsg("Garage door just opened ! (battery : "+String(vin,2)+" v");
  //String mess = ConvertToJSon(String(vin,2));
  debug_message("JSON Sensor : " + mess + ", topic : " +SMS_TOPIC, true);
  mess.toCharArray(message_buff, mess.length()+1);
    
  client.publish(SMS_TOPIC,message_buff);

  //Turn off LED... 
  digitalWrite(LED_PIN, HIGH);

  disconnectMqtt();
  delay(100);
  disconnectWifi();
  delay(100);

  weAreDone();
}
