/**** 
 * homehub_v2
 *  Compile with board LOLIN(WEMOS) D1 R2 & mini on Arduino IDE 1.8.13
 * 
 * F. Guiet 
 * Creation           : End of 2018
 * Last modification  : 20201018
 * 
 * Version            : 1.4
 * 
 * History            : Huge refactoring - add mqtt toppic for each sensor
 *                      Remove first / in topic, fix bug in mqtt connect (possible infinite loop), improve debug_message function
 *                      20190310 - Add main entry door reedswitch sensor
 *                      1.4 - 20201018 - Move to Arduino Json 6 and change office sensor mac address
 *                      
 *                      
 */

//Software serial (allow debugging...)
#include <SoftwareSerial.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

#define SOFTSERIAL_TX D5 //=D1 on Wemos
#define SOFTSERIAL_RX D2 //=D2 on Wemos
#define DEBUG 1
#define MAX_RETRY 50
#define MQTT_SERVER "192.168.1.25"

//*** CHANGE IT
//#define MQTT_CLIENT_ID "HubUpstairsMqttClient"
#define MQTT_CLIENT_ID "HubDownstairsMqttClient"
//#define MQTT_HUB_TOPIC "guiet/upstairs/hub"
#define MQTT_HUB_TOPIC "guiet/downstairs/hub"
#define FIRMWARE_VERSION "1.4"
#define MQTT_HUB_MESSAGE "HUB_DOWNSTAIRS_ALIVE"
//#define MQTT_HUB_MESSAGE "HUB_UPSTAIRS_ALIVE"

SoftwareSerial softSerial(SOFTSERIAL_RX, SOFTSERIAL_TX); // RX, TX

const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

const int SERIAL_BAUD = 9600;
const int serialBufSize = 100;      //size for at least the size of incoming JSON string
static char serialbuffer[serialBufSize]; 
//const int pinHandShake = D3; //handshake pin, Wemos R1 Mini D3
unsigned long previousMillis=0;
const int INTERVAL = 30000; //Every 30s
char message_buff[100];

WiFiClient espClient;
PubSubClient client(espClient);

struct Sensor {
    String Address;
    String Name;    
    String SensorId;
    String Temperature;
    String Humidity;
    String Battery;
    String Rssi;
    String Mqtt_topic;
    String State;
    String Type;
};

//*** CHANGE IT
#define SENSORS_COUNT 3

Sensor sensors[SENSORS_COUNT];

void InitSensors() {

  //String SENSORID =  "1"; //Bureau
  //String SENSORID =  "2"; //Salon
  //String SENSORID =  "3"; //Nohe
  //String SENSORID =  "4"; //Manon
  //String SENSORID =  "5"; //Parents
  //String SENSORID =  "12"; //Bathroom (upstairs)
  
  /*
  sensors[0].Address = "d2:48:c8:a5:35:4c";
  sensors[0].Name = "Manon";
  sensors[0].SensorId = "4";
  sensors[0].Mqtt_topic = "guiet/upstairs/room_manon/sensor/4";
  sensors[0].Type = "Environmental";
  
  sensors[1].Address = "c7:b9:43:94:24:3a";
  sensors[1].Name = "Nohé";
  sensors[1].SensorId = "3";
  sensors[1].Mqtt_topic = "guiet/upstairs/room_nohe/sensor/3";
  sensors[1].Type = "Environmental";
  
  sensors[2].Address = "e9:3d:63:97:39:5e";
  sensors[2].Name = "Parents";
  sensors[2].SensorId = "5";
  sensors[2].Mqtt_topic = "guiet/upstairs/room_parents/sensor/5";
  sensors[2].Type = "Environmental";

  sensors[3].Address = "d8:15:dc:ff:2c:4d";
  sensors[3].Name = "Bathroom";
  sensors[3].SensorId = "12";
  sensors[3].Mqtt_topic = "guiet/upstairs/bathroom/sensor/12";
  sensors[3].Type = "Environmental";
  */
  
  sensors[0].Address = "d7:e0:cf:0e:99:c1";
  sensors[0].Name = "Bureau";
  sensors[0].SensorId = "1";
  sensors[0].Mqtt_topic = "guiet/downstairs/office/sensor/1";
  sensors[0].Type = "Environmental";

  sensors[1].Address = "f4:a4:c6:6f:d8:6a";
  sensors[1].Name = "Salon";
  sensors[1].SensorId = "2";
  sensors[1].Mqtt_topic = "guiet/downstairs/livingroom/sensor/2";
  sensors[1].Type = "Environmental";

  sensors[2].Address = "c5:f7:7b:9b:24:46";
  sensors[2].Name = "Reedswitch - porte entrée";
  sensors[2].SensorId = "16";
  sensors[2].Mqtt_topic = "guiet/downstairs/livingroom/sensor/16";
  sensors[2].Type = "Reedswitch";
}


void setup() {
  Serial.begin(SERIAL_BAUD); //ESP8266 default serial on UART0 is GPIO1 (TX) and GPIO3 (RX)
  softSerial.begin(SERIAL_BAUD); // to AltSoftSerial RX

  InitSensors();
  
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

int getSensorByAddress(String address, Sensor &sensor) {
  for(int i=0;i<SENSORS_COUNT;i++) {
    if (sensors[i].Address == address) {
      sensor = sensors[i];
      debug_message("Found : " + sensor.SensorId, true);
      return 1;
    }
  }

  return 0;
}

//stealing this code from
//https://hackingmajenkoblog.wordpress.com/2016/02/01/reading-serial-on-the-arduino/
//non-blocking serial readline routine, very nice.  Allows mqtt loop to run.
int readline(int readch, char *buffer, int len)
{
  static int pos = 0;
  int rpos;

  if (readch > 0) {
    switch (readch) {
      case '\r': // Ignore new-lines
        break;
      case '\n': // Return on CR
        rpos = pos;
        pos = 0;  // Reset position index ready for next time
        return rpos;
      default:
        if (pos < len-1) {
          buffer[pos++] = readch;   //first buffer[pos]=readch; then pos++;
          buffer[pos] = 0;
        }
    }
  }
  // No end of line has been found, so return -1.
  return -1;
}

String ConvertToJSon(Sensor sensor) {
    //Create JSon object
    //DynamicJsonBuffer  jsonBuffer(200);
    //JsonObject& root = jsonBuffer.createObject();
    DynamicJsonDocument  jsonBuffer(200);
    JsonObject root = jsonBuffer.to<JsonObject>();
    
    root["sensorid"] = sensor.SensorId;
    root["name"] = sensor.Name;
    root["firmware"]  = FIRMWARE_VERSION;
    root["rssi"] = sensor.Rssi;
    root["battery"] = sensor.Battery;
    root["state"] = sensor.State;
    
    String result;    
    serializeJson(root, result);

    return result;
}

void jsonParser(char *buffer) {
  DynamicJsonDocument  jsonObj(200);
  //JsonObject& jsonObj = jsonBuffer.parseObject(buffer);
  //JsonObject jsonObj = jsonBuffer.to<JsonObject>();
  auto error = deserializeJson(jsonObj, buffer);

  if (!error)
  {
    Sensor sensor;
    int val = getSensorByAddress(jsonObj["Address"].as<String>(), sensor);

    if (val != 0) {

      if (sensor.Type == "Reedswitch") {
      
          sensor.Battery = jsonObj["Battery"].as<String>();
          sensor.State = jsonObj["State"].as<String>();
          sensor.Rssi = jsonObj["Rssi"].as<String>();

          String mess = ConvertToJSon(sensor);
      
          debug_message("Publishing : " + mess, true);
                      
          mess.toCharArray(message_buff, mess.length()+1);
          
          client.publish(sensor.Mqtt_topic.c_str() ,message_buff);
        
      }

      if (sensor.Type == "Environmental") {
      
          sensor.Temperature = jsonObj["Temperature"].as<String>();
          sensor.Battery = jsonObj["Battery"].as<String>();
          sensor.Humidity = jsonObj["Humidity"].as<String>();
          sensor.Rssi = jsonObj["Rssi"].as<String>();
      
          String mess = "SETINSIDEINFO;"+sensor.SensorId+";"+sensor.Temperature+";"+sensor.Humidity+";"+sensor.Battery+";"+sensor.Rssi;
      
          debug_message("Publishing : " + mess, true);
                      
          mess.toCharArray(message_buff, mess.length()+1);
          
          client.publish(sensor.Mqtt_topic.c_str() ,message_buff);
      }    
    }
  }
  else {
    debug_message("Error parsing : " + String(buffer), true);
  }
}

void loop() {

  if (WiFi.status() != WL_CONNECTED) {
      digitalWrite(BUILTIN_LED, LOW);  //LED on
      connectToWifi();
      digitalWrite(BUILTIN_LED, HIGH);  //LED off
  }

  if (!client.connected()) {
    connectToMqtt();
  }

   client.loop();
  
  if (softSerial.available() > 0) {
  //if (Serial.available() > 0) {
    //received serial line of json
    if (readline(softSerial.read(), serialbuffer, serialBufSize) > 0)
    {
      //digitalWrite(pinHandShake, HIGH);
      digitalWrite(BUILTIN_LED, LOW);  //LED on
      
      debug_message("You entered: >", false);
      debug_message(serialbuffer, false);
      debug_message("<", true);

      jsonParser(serialbuffer);
      
      digitalWrite(BUILTIN_LED, HIGH);  //LED off
      //digitalWrite(pinHandShake, LOW);
    }
  }

  unsigned long currentMillis = millis();
  if (((unsigned long)(currentMillis - previousMillis) >= INTERVAL) || ((unsigned long)(millis() - previousMillis) < 0)) {
    
    //Publish alive topic every 30s
    String mess = String(MQTT_HUB_MESSAGE) + ";" + String(FIRMWARE_VERSION) + ";" + String(ESP.getFreeHeap()); 
    mess.toCharArray(message_buff, mess.length()+1);
    client.publish(MQTT_HUB_TOPIC,message_buff);

    debug_message("Publishing : " + mess, true);

    // Save the current time to compare "later"
    previousMillis = currentMillis;
  }
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 

  int retry = 0;

  debug_message("Attempting MQTT connection...", true);
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      debug_message("connected to MQTT Broker...", true);
    }
    else {
      delay(500);
      debug_message(".", false);
      retry++;
    }

    if (retry >= MAX_RETRY) {
      ESP.restart();
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

    debug_message(".", true);
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

void makeLedBlink(int blinkTimes, int millisecond) {

  for (int x = 0; x < blinkTimes; x++) {
    digitalWrite(LED_BUILTIN, HIGH);
    delay(millisecond);
    digitalWrite(LED_BUILTIN, LOW);
    delay(millisecond);
  } 
}
