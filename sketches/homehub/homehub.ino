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
 * In fact, this library does not have getServiceNb() function and getServiceData(int i)  
 */

#include "BLEDevice.h"
#include <WiFi.h>
#include <PubSubClient.h>
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

#define SENSORS_COUNT 3
#define DEBUG 1

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

bool deviceFound = false;
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
}

long StrToHex(char str[])
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

          sensors[i].hasDataToPush = true;

          if (DEBUG) {
            Serial.printf("Device found with Mac Address : %s \r\n", pServerAddress->toString().c_str());
            Serial.println("Getting service data...");
          }
         
           if (advertisedDevice.haveServiceData()) {

              //String name = "noname";

              //if (advertisedDevice.haveName())
              //  name = advertisedDevice.getName();

              int rssi = 0;

              if (advertisedDevice.haveRSSI())
                rssi = advertisedDevice.getRSSI();

              sensors[i].Rssi = rssi;
                
              int cpt = advertisedDevice.getServiceDataCount();

              if (DEBUG) {
                Serial.println("Service data found : " + String(cpt));
              }
              
              char serviceData[10];
              for(int j=0;j<cpt;j++) {

                if (DEBUG)
                   Serial.println("Sensor : " + sensors[i].Name);
                   //Serial.println("Name : " + name);
                   Serial.println("Service n°" + String(j));
                std::string strServiceData = advertisedDevice.getServiceData(j);                
                strServiceData.copy(serviceData, strServiceData.length(), 0);

                /* Allocate twice the number of the bytes in the buf array because each byte would be 
                 * converted to two hex characters, also add an extra space for the terminating null byte
                 * [size] is the size of the buf array */
                char output[(strServiceData.length() * 2) + 1];
                /* pointer to the first item (0 index) of the output array */
                char *ptr = &output[0];
                
                for (int i = 0; i < strServiceData.length(); i++) {
                    /* sprintf converts each byte to 2 chars hex string and a null byte, for example
                     * 10 => "0A\0".
                     *
                     * These three chars would be added to the output array starting from
                     * the ptr location, for example if ptr is pointing at 0 index then the hex chars
                     * "0A\0" would be written as output[0] = '0', output[1] = 'A' and output[2] = '\0'.
                     *
                     * sprintf returns the number of chars written execluding the null byte, in our case
                     * this would be 2. Then we move the ptr location two steps ahead so that the next
                     * hex char would be written just after this one and overriding this one's null byte.
                     *
                     * We don't need to add a terminating null byte because it's already added from 
                     * the last hex string. */  
                    ptr += sprintf (ptr, "%02X", serviceData[strServiceData.length()-i-1]);
                }

                
                String t = "0x" + String(output);
                if (DEBUG) {
                  Serial.println("Hex String : " + t);
                }               
                long serviceValue = StrToHex(output);
                float serviceValueFloat = String(serviceValue).toFloat();

                if (DEBUG) {
                  printf ("Output (long) : %ld\n", serviceValue);
                  Serial.println("Output (float) : " + String(serviceValueFloat,2));
                }
                  
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
  

     /* bool known = false;
      
      
        if (known) {
        Serial.print("Device found: ");
        Serial.println(advertisedDevice.getRSSI());
        if (advertisedDevice.getRSSI() > -80) deviceFound = true;
        else deviceFound = false;
        Serial.println(pServerAddress->toString().c_str());
        advertisedDevice.getScan()->stop();
      }*/
    }
}; // MyAdvertisedDeviceCallbacks

void setup() {

  if (DEBUG)
    Serial.begin(115200);
    Serial.println("Starting Arduino BLE Client application...");

  if (DEBUG) {
     Serial.println("Disabling brown detector...");
  }
  WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); //disable brownout detector

  BLEDevice::init(BLE_CLIENT_ID);

  pClient  = BLEDevice::createClient();
  //Serial.println(" - Created client");
  pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);

  InitSensors();
}

void loop() {

  if (DEBUG) {
    Serial.print("Free HeapSize: ");
    Serial.println(esp_get_free_heap_size());
  }
  
 // Serial.println("Sensors : " + sizeof(sensors));

  if (DEBUG) {
    Serial.println();
    Serial.println("BLE Scan restarted.....");
  }
  
  deviceFound = false;
  BLEScanResults scanResults = pBLEScan->start(20);
  
  if (deviceFound && DEBUG) {
    Serial.println("Found device");
  }

  pBLEScan->stop();

  if (DEBUG) 
    Serial.println("BLE Scan ended.....");
  
  delay(2000);

  connectToWifi();

  connectToMqtt();

  //Publish alive topic
  String mess1 = "UPSTAIRS_HUB_ALIVE";
  mess1.toCharArray(message_buff, mess1.length()+1);
  client.publish(MQTT_HUB_TOPIC,message_buff);

  //Publish result
  for (int i=0; i < SENSORS_COUNT;i++) {
    if (sensors[i].hasDataToPush) {
      sensors[i].hasDataToPush = false;
      String mess = "SETINSIDEINFO;"+String(sensors[i].SensorId)+";"+sensors[i].Temperature+";"+sensors[i].Humidity+";"+sensors[i].Battery+";"+String(sensors[i].Rssi);

      if (DEBUG) {
        Serial.println("Publishing : " + mess);
      }
          
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

  if (DEBUG)
    Serial.println("Restarting...");
  //Memory leak...
  ESP.restart();
}

void disconnectMqtt() {
  if (DEBUG)
    Serial.println("Disconnecting from mqtt...");
  client.disconnect();
}

void disconnectWifi() {
  if (DEBUG)
    Serial.println("Disconnecting from wifi...");
  WiFi.disconnect();
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 

  int retry = 0;

  if (DEBUG)
    Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      if (DEBUG)
        Serial.println("connected to MQTT Broker...");
    }
    else {
      delay(500);

      if (DEBUG)
        Serial.print(".");
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

  if (DEBUG)
    Serial.println("Connecting to WiFi...");
//  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    delay(500);

    if (DEBUG)
       Serial.print(".");
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
