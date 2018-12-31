/*
 * This program use a modified version of :
 *    https://github.com/nkolban/ESP32_BLE_Arduino
 *    
 * Interesting links:   
 * https://github.com/nkolban/esp32-snippets/issues/168
 * https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLETests/SampleAsyncScan.cpp
 * https://github.com/nkolban/esp32-snippets/issues/496
 * https://github.com/nkolban/esp32-snippets/issues/651
 * 
 * Hex online converter
 * https://www.scadacore.com/tools/programming-calculators/online-hex-converter/
 *    
 * In fact, this library does not have getServiceNb() function and getServiceData(int i)  
 */

#include "BLEDevice.h"
#include <WiFi.h>
#include <PubSubClient.h>
#include <errno.h>
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

#define SENSORS_COUNT 3
#define DEBUG 0

const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

#define MAX_RETRY 50
#define MQTT_CLIENT_ID "HubUpstairsMqttClient"
#define MQTT_SERVER "192.168.1.25"
#define MQTT_TOPIC "/guiet/inside/sensor"
#define MQTT_HUB_TOPIC "/guiet/upstairs/hub"
//#define MQTT_HUB_TOPIC "/guiet/downstairs/hub"

#define BLE_CLIENT_ID "HubUpstairsBleClient"
BLEScan* pBLEScan;
BLEClient* pClient;

WiFiClient espClient;
PubSubClient client(espClient);

static BLEAddress *pServerAddress;
char message_buff[100];

struct Sensor {
    String Address;
    String Name;
    int SensorId;
    String Temperature;
    String Humidity;
    String Battery;
    bool hasDataToPush;
    int Rssi;
};

Sensor sensors[SENSORS_COUNT];

void InitSensors() {

  //String SENSORID =  "1"; //Bureau
  //String SENSORID =  "2"; //Salon
  //String SENSORID =  "3"; //Nohe
  //String SENSORID =  "4"; //Manon
  //String SENSORID =  "5"; //Parents
  
  sensors[0].Address = "d2:48:c8:a5:35:4c";
  sensors[0].Name = "Manon";
  sensors[0].SensorId = 4;
  sensors[0].hasDataToPush = false;

  sensors[1].Address = "c7:b9:43:94:24:3a";
  sensors[1].Name = "Nohé";
  sensors[1].SensorId = 3;
  sensors[1].hasDataToPush = false;

  sensors[2].Address = "e9:3d:63:97:39:5e";
  sensors[2].Name = "Parents";
  sensors[2].SensorId = 5;
  sensors[2].hasDataToPush = false;

  /*sensors[3].Address = "c5:f7:7b:9b:24:46";
  sensors[3].Name = "Bureau";
  sensors[3].SensorId = 1;
  sensors[3].hasDataToPush = false;*/
}

long HexToLong(char str[])
{
  return strtol(str, 0, 16);
}

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  /**
      Called for each advertising BLE server.
  */
  void onResult(BLEAdvertisedDevice advertisedDevice) {
      
      pServerAddress = new BLEAddress(advertisedDevice.getAddress());

      for (int i = 0; i < SENSORS_COUNT; i++) {
        if (strcmp(pServerAddress->toString().c_str(), sensors[i].Address.c_str()) == 0) {

          //Reset sensor data
          sensors[i].hasDataToPush = true;
          
          if (DEBUG) {
            Serial.printf("Device found with Mac Address : %s \r\n", pServerAddress->toString().c_str());      
          }

          debug_message("Getting service data...");
         
          if (advertisedDevice.haveServiceData()) {

              int rssi = 0;

              if (advertisedDevice.haveRSSI())
                rssi = advertisedDevice.getRSSI();

              sensors[i].Rssi = rssi;
                
              int cpt = advertisedDevice.getServiceDataCount();

              debug_message("Service data found : " + String(cpt));
              
              for(int j=0;j<cpt;j++) {

                debug_message("Sensor : " + sensors[i].Name);
                debug_message("Service n°" + String(j));
                
                std::string strServiceData = advertisedDevice.getServiceData(j); 

                String serviceDataString = (String)BLEUtils::buildHexData(nullptr, (uint8_t *)advertisedDevice.getServiceData(j).data(), advertisedDevice.getServiceData(j).length());

                //Handle length <> 2 or 4
                //check data received
                if (serviceDataString.length() != 4 && serviceDataString.length() != 2) {
                  continue;
                }
                
                //Reverse order big endian
                if (serviceDataString.length() == 4) {
                   serviceDataString = serviceDataString.substring(2,4) + serviceDataString.substring(0,2);
                }

                debug_message("Service Data String : " + serviceDataString);

                char copy[10];
                serviceDataString.toCharArray(copy,serviceDataString.length()+1);

                /* reset errno to 0 before call */
                errno = 0;

                long serviceValue = HexToLong(copy);

                if (errno != 0) continue;
                
                float serviceValueFloat = String(serviceValue).toFloat();

                if (DEBUG) {
                  printf ("Output (long) : %ld\n", serviceValue);
                }

                debug_message("Output (float) : " + String(serviceValueFloat,2));
                 
                //Temperature
                if (j == 0)
                  sensors[i].Temperature = String(serviceValueFloat/100,2);

                //Battery
                if (j == 1)
                  sensors[i].Battery = String(serviceValueFloat,0);

                //Humidity
                if (j == 2)
                  sensors[i].Humidity = String(serviceValueFloat,0);
             }
           }
        }
      }
    }
}; // MyAdvertisedDeviceCallbacks

void debug_message(String message) {
  if (DEBUG) {
    Serial.println(message);
  }
}

void setup() {

  if (DEBUG) {
    Serial.begin(115200);
  }

  //Sanity delay
  delay(1000);

  debug_message("Starting Arduino BLE Client application...");
  debug_message("Disabling brown detector...");
  
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); //disable brownout detector

  BLEDevice::init(BLE_CLIENT_ID);

  pClient  = BLEDevice::createClient();
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);

  InitSensors();
}

void loop() {

  debug_message("Free HeapSize: ");
  debug_message(String(esp_get_free_heap_size()));
  
  debug_message("BLE Scan restarted.....");
  
  BLEScanResults scanResults = pBLEScan->start(10);
  
  pBLEScan->stop();

  debug_message("BLE Scan ended.....");
  
  delay(2000);

  connectToWifi();

  connectToMqtt();

  //Publish alive topic
  String mess = "UPSTAIRS_HUB_ALIVE";
  mess.toCharArray(message_buff, mess.length()+1);
  client.publish(MQTT_HUB_TOPIC,message_buff);

  //Publish result
  for (int i=0; i < SENSORS_COUNT;i++) {
    if (sensors[i].hasDataToPush) {
      sensors[i].hasDataToPush = false;
      mess = "SETINSIDEINFO;"+String(sensors[i].SensorId)+";"+sensors[i].Temperature+";"+sensors[i].Humidity+";"+sensors[i].Battery+";"+String(sensors[i].Rssi);

      debug_message("Publishing : " + mess);
                
      mess.toCharArray(message_buff, mess.length()+1);
      client.publish(MQTT_TOPIC,message_buff);
    }
  }

  //Disconnect pro
  if (client.connected()) {
    disconnectMqtt();  
  }

  if (WiFi.status() == WL_CONNECTED) {
    disconnectWifi();  
  }

  delay(1000);
  debug_message("Restarting...");
  
  //Memory leak...
  ESP.restart();

  delay(500); //Useful?
}

void disconnectMqtt() {
  debug_message("Disconnecting from mqtt...");
  client.disconnect();
}

void disconnectWifi() {
  debug_message("Disconnecting from wifi...");
  WiFi.disconnect();
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 

  int retry = 0;

  debug_message("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      debug_message("connected to MQTT Broker...");
    }
    else {
      delay(500);

      debug_message(".");
    }
  }

  if (retry >= MAX_RETRY) {
    ESP.restart();
  }
}

void connectToWifi() 
{
  byte mac[6];

  WiFi.disconnect();

  debug_message("Connecting to WiFi...");
//  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    delay(500);

    debug_message(".");
    retry++;    
  }

  if (retry >= MAX_RETRY)
    ESP.restart();

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
