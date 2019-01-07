#include <ESP8266HTTPClient.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>

#define MAX_RETRY 50
#define MQTT_TOPIC "/guiet/anemometer/windspeed"
#define MQTT_CLIENT_ID "AnemometerSensor"
#define MQTT_SERVER "192.168.1.25"

IPAddress ip_wemos (192,168,1,200); //Wemos
IPAddress gateway_ip ( 192,168,1,1);
IPAddress subnet_mask(255, 255, 255,0);
const char* ssid = "DUMBLEDORE";
const char* password = "frederic";

//const int CALIBRATION = 1;
//const char* ssid = "ANEMOMETER-CALIBRATION-AP";
//String SEND_WIND_SPEED_URL = "http://192.168.4.1:80/";

const int HALL_SENSOR_PIN = D5;
int RECORD_WIND_SPEED_TIME_DURATION = 5000; //in ms
const int SLEEP_TIME_SECONDS = 1800 ;

volatile int rpmcount = 0;
volatile unsigned long contactBounceTime; // timer to avoid contact bounce in wind speed sensor

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);

  pinMode(HALL_SENSOR_PIN, INPUT_PULLUP); 
  //pinMode(LED_BUILTIN , OUTPUT); //set LED pin as output 

  Serial.println("Ready...");

   //Connecting to WiFi
  connectToWifi();

  //Connect to Mqtt
  connectToMqtt();   
}

void loop() {

  float windspeed = getWindSpeed();
  
  String mess = "SETANEMOMETER;"+String(windspeed,2);  
  client.publish(MQTT_TOPIC,mess.c_str());

  //Deep sleep...ZZzzzZZzzz
  goDeepSleep();  

  //delay(500);
  
  /*if (WiFi.status() != WL_CONNECTED && CALIBRATION == 1) {
    connectToWifi();  
    sendHttpRequest(SEND_WIND_SPEED_URL + "Connected");
  }

  Serial.println("Reading Wind Speed");
  float windspeed = getWindSpeed();

  sendHttpRequest(SEND_WIND_SPEED_URL + String(rpmcount));
  
  Serial.println("rpm : " + String(rpmcount));
  Serial.println("wind speed : " + String(windspeed,2));*/
  
}

void connectionOk() {
  for(int i=0;i<=5;i++) {
    digitalWrite(LED_BUILTIN, HIGH);
    delay(500);
    digitalWrite(LED_BUILTIN, LOW);
    delay(500);
  }
}

void sendHttpRequest(String request) {
  HTTPClient httpClient;
  httpClient.setTimeout(2000);
  httpClient.begin(request);
  int httpCode = httpClient.GET();  
}

void rotationCounter() {
  if((millis() - contactBounceTime) > 15 ) { // debounce the switch contact
    rpmcount++;
    contactBounceTime = millis();
  }  
}

float getWindSpeed() {

  rpmcount = 0;
  float windspeed = 0;
  unsigned long lastmillis = millis();

  attachInterrupt(digitalPinToInterrupt(HALL_SENSOR_PIN), rotationCounter, RISING);

  delay(500);
  
  //Record wind speed 
  while (millis() - lastmillis < RECORD_WIND_SPEED_TIME_DURATION) {
     delay(1);
  }

  detachInterrupt(digitalPinToInterrupt(HALL_SENSOR_PIN)); 
  
  //Compute wind speed
  //V=2*pi*R*N*F
  //V : vitesse du vent [m/s]
  //R : rayon moyen des bras (de l’axe de rotation jusqu’au centre des coupelles) [m]
  //N : nombre de tours par seconde [1/s]
  //F : fonction d’étalonnage
  //windspeed = 2*3.14159*0.09*(rpmcount / 5);
  //windspeed = windspeed * 3.6; //Convert m/s in km/h  

  //Use my car to get rpm vs speed in km/h
  //Then I use Excel to get the regression linear formula
  windspeed = (1.019 * rpmcount) - 9.5476;

  if (windspeed < 0) windspeed=0;

  return windspeed;
}

void connectToMqtt() {
  
  client.setServer(MQTT_SERVER, 1883); 

  int retry = 0;
  Serial.print("Attempting MQTT connection...");
  while (!client.connected()) {   
    if (client.connect(MQTT_CLIENT_ID)) {
      Serial.println("connected to MQTT Broker...");
    }
    else {
      delay(500);
      Serial.print(".");
    }
  }

  if (retry >= MAX_RETRY) {
    goDeepSleep();
  }
}

void connectToWifi() 
{
  byte mac[6];

  WiFi.forceSleepWake();
  WiFi.disconnect();
  Serial.println("Connecting to WiFi...");
  WiFi.config(ip_wemos, gateway_ip, subnet_mask);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  int retry = 0;
  while (WiFi.status() != WL_CONNECTED && retry < MAX_RETRY) {
    delay(500);
    Serial.print(".");
    retry++;    
  }

  if (retry >= MAX_RETRY)
    goDeepSleep();

  //connectionOk();
  
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

void disconnectMqtt() {
  Serial.println("Disconnecting from mqtt...");
  client.disconnect();
}

void disconnectWifi() {
  Serial.println("Disconnecting from wifi...");
  WiFi.disconnect();
}

void goDeepSleep() {
  //Disconnect pro
  if (client.connected()) {
    disconnectMqtt();  
  }

  if (WiFi.status() == WL_CONNECTED) {
    disconnectWifi();  
  }  

  Serial.println("Entering deep sleep mode...good dreams fellows...ZzzzzZzzZZZZzzz");
  ESP.deepSleep(SLEEP_TIME_SECONDS * 1000000);
  delay(200); //Recommanded
}

